package com.castfan.repository;

import java.util.List;
import java.util.Optional;

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

	@Query("select profile from MemberProfile profile join fetch profile.userAccount account where lower(account.email) = lower(:email)")
	Optional<MemberProfile> findByUserAccountEmailIgnoreCase(@Param("email") String email);

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
