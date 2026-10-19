package com.castfan.repository;

import java.time.Instant;
import java.util.Optional;

import com.castfan.entity.StorePromotionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StorePromotionImageRepository extends JpaRepository<StorePromotionImage, Long> {
	@Query("select image from StorePromotionImage image where image.storePromotion.id = :id and lower(image.storePromotion.store.owner.email) = lower(:email)")
	Optional<StorePromotionImage> findOwned(@Param("id") Long id, @Param("email") String email);

	@Query("select image from StorePromotionImage image where image.storePromotion.id = :id and image.storePromotion.active = true and image.storePromotion.store.active = true and image.storePromotion.startsAt <= :now and image.storePromotion.endsAt > :now")
	Optional<StorePromotionImage> findPublic(@Param("id") Long id, @Param("now") Instant now);
}
