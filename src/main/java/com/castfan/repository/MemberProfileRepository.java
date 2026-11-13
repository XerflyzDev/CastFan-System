package com.castfan.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.castfan.entity.MemberProfile;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Long> {

	Optional<MemberProfile> findByUserAccountId(Long userAccountId);

	@Query("select profile from MemberProfile profile join fetch profile.userAccount account where lower(account.loginIdentifier) = lower(:identifier)")
	Optional<MemberProfile> findByUserAccountLoginIdentifierIgnoreCase(@Param("identifier") String identifier);

	@Query(value = """
		select profile from MemberProfile profile
		join fetch profile.userAccount account
		where account.role = :role and account.status = :status
		  and (:query = '' or lower(account.displayName) like concat('%', :query, '%')
		    or lower(account.email) like concat('%', :query, '%'))
		order by lower(account.displayName), lower(account.email), profile.id
		""",
		countQuery = """
		select count(profile) from MemberProfile profile join profile.userAccount account
		where account.role = :role and account.status = :status
		  and (:query = '' or lower(account.displayName) like concat('%', :query, '%')
		    or lower(account.email) like concat('%', :query, '%'))
		""")
	Page<MemberProfile> searchActiveMembers(@Param("query") String query, @Param("role") UserRole role,
		@Param("status") UserStatus status, Pageable pageable);

	boolean existsByUserAccountId(Long userAccountId);

	@Query("""
		select profile from MemberProfile profile
		join fetch profile.userAccount account
		where account.role = :role
		  and account.status = :status
		  and not exists (
			select castProfile.id from CastProfile castProfile
			where castProfile.userAccount.id = account.id
		  )
		order by account.displayName asc, account.email asc
		""")
	List<MemberProfile> findCastPromotionCandidates(
		@Param("role") UserRole role,
		@Param("status") UserStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select profile from MemberProfile profile where profile.id = :id")
	Optional<MemberProfile> findByIdForUpdate(@Param("id") Long id);
}
