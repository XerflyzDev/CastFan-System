package com.castfan.repository;

import java.util.List;
import java.util.Optional;

import com.castfan.entity.CastContent;
import com.castfan.entity.ContentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CastContentRepository extends JpaRepository<CastContent, Long> {

	@Query("""
		select content from CastContent content
		left join fetch content.minimumTier tier
		where lower(content.castProfile.userAccount.email) = lower(:email)
		order by content.createdAt desc
		""")
	List<CastContent> findAllOwned(@Param("email") String email);

	@Query("""
		select content from CastContent content
		left join fetch content.minimumTier tier
		where content.castProfile.id = :castProfileId and content.status = :status
		order by content.publishedAt desc
		""")
	List<CastContent> findPublishedForProfile(
		@Param("castProfileId") Long castProfileId,
		@Param("status") ContentStatus status
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select content from CastContent content left join fetch content.minimumTier where content.id = :id")
	Optional<CastContent> findByIdForUpdate(@Param("id") Long id);
}
