package com.castfan.repository;

import java.util.List;
import java.util.Optional;

import com.castfan.entity.CastContent;
import com.castfan.entity.ContentStatus;
import com.castfan.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface CastContentRepository extends JpaRepository<CastContent, Long> {

	@Query("""
		select content from CastContent content
		where lower(content.castProfile.userAccount.loginIdentifier) = lower(:email)
		order by content.createdAt desc
		""")
	List<CastContent> findAllOwned(@Param("email") String email);

	@Query("""
		select content from CastContent content
		where content.castProfile.id = :castProfileId and content.status = :status
		order by content.publishedAt desc
		""")
	List<CastContent> findPublishedForProfile(
		@Param("castProfileId") Long castProfileId,
		@Param("status") ContentStatus status
	);

	@Query("""
		select content from CastContent content
		join fetch content.castProfile profile
		join fetch profile.userAccount account
		where content.status = :status
		  and account.status = :userStatus
		  and exists (
			select follow.id from CastFollow follow
			where follow.memberProfile.id = :memberProfileId and follow.castProfile.id = profile.id
		  )
		order by content.publishedAt desc
		""")
	List<CastContent> findFeedForMember(
		@Param("memberProfileId") Long memberProfileId,
		@Param("status") ContentStatus status,
		@Param("userStatus") UserStatus userStatus,
		Pageable pageable
	);

	@Query("""
		select content from CastContent content
		join fetch content.castProfile profile
		join fetch profile.userAccount account
		where content.store.id = :storeId and content.status = :status and account.status = :userStatus
		  and exists (select follow.id from CastFollow follow
		      where follow.storeMembership.id = :membershipId and follow.castProfile.id = profile.id)
		order by content.publishedAt desc
		""")
	List<CastContent> findFeedForStoreMembership(@Param("storeId") Long storeId,
		@Param("membershipId") Long membershipId, @Param("status") ContentStatus status,
		@Param("userStatus") UserStatus userStatus, Pageable pageable);

	@Query("""
		select content from CastContent content
		join fetch content.castProfile profile
		join fetch profile.userAccount account
		where content.id = :contentId
		  and profile.id = :castProfileId
		  and content.status = :status
		  and account.status = :userStatus
		""")
	Optional<CastContent> findPublishedByIdForProfile(
		@Param("contentId") Long contentId,
		@Param("castProfileId") Long castProfileId,
		@Param("status") ContentStatus status,
		@Param("userStatus") UserStatus userStatus
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select content from CastContent content where content.id = :id")
	Optional<CastContent> findByIdForUpdate(@Param("id") Long id);
}
