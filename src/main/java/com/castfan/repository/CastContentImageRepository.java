package com.castfan.repository;

import java.util.Optional;

import com.castfan.entity.CastContentImage;
import com.castfan.entity.ContentStatus;
import com.castfan.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CastContentImageRepository extends JpaRepository<CastContentImage, Long> {
	@Query("select image from CastContentImage image where image.castContent.id = :id and lower(image.castContent.castProfile.userAccount.email) = lower(:email)")
	Optional<CastContentImage> findOwned(@Param("id") Long id, @Param("email") String email);

	@Query("select image from CastContentImage image where image.castContent.id = :id and image.castContent.castProfile.id = :profileId and image.castContent.status = :status and image.castContent.castProfile.discoverable = true and image.castContent.castProfile.userAccount.status = :userStatus")
	Optional<CastContentImage> findPublic(@Param("id") Long id, @Param("profileId") Long profileId,
		@Param("status") ContentStatus status, @Param("userStatus") UserStatus userStatus);
}
