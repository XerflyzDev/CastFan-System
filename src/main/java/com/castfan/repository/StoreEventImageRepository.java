package com.castfan.repository;

import java.util.Optional;

import com.castfan.entity.EventStatus;
import com.castfan.entity.StoreEventImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreEventImageRepository extends JpaRepository<StoreEventImage, Long> {

	@Query("""
		select image
		from StoreEventImage image
		join image.storeEvent event
		join event.store store
		where event.id = :eventId
		  and lower(store.owner.email) = lower(:email)
		""")
	Optional<StoreEventImage> findOwnedImage(@Param("eventId") Long eventId, @Param("email") String email);

	@Query("""
		select image
		from StoreEventImage image
		join image.storeEvent event
		join event.store store
		where event.id = :eventId
		  and event.status = :status
		  and store.active = true
		""")
	Optional<StoreEventImage> findPublicImage(
		@Param("eventId") Long eventId,
		@Param("status") EventStatus status
	);
}
