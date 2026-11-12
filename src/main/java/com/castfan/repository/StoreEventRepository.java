package com.castfan.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.castfan.entity.EventStatus;
import com.castfan.entity.StoreEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface StoreEventRepository extends JpaRepository<StoreEvent, Long> {
	List<StoreEvent> findAllByStoreIdAndStatusAndEndsAtAfterOrderByStartsAtAsc(
		Long storeId, EventStatus status, Instant now, Pageable pageable);

	@Query("""
		select event
		from StoreEvent event
		join fetch event.store store
		where lower(store.owner.email) = lower(:email)
		order by event.startsAt desc
		""")
	List<StoreEvent> findAllForOwner(@Param("email") String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select event
		from StoreEvent event
		join fetch event.store store
		where event.id = :eventId
		""")
	Optional<StoreEvent> findByIdForUpdate(@Param("eventId") Long eventId);

	@Query("""
		select count(event)
		from StoreEvent event
		where lower(event.store.owner.email) = lower(:email)
		  and event.status = :status
		  and event.endsAt > :now
		""")
	long countUpcomingForOwner(
		@Param("email") String email,
		@Param("status") EventStatus status,
		@Param("now") Instant now
	);

	@Query("""
		select event
		from StoreEvent event
		join fetch event.store store
		where event.id = :eventId
		  and event.status = :status
		  and store.active = true
		""")
	Optional<StoreEvent> findPublicById(
		@Param("eventId") Long eventId,
		@Param("status") EventStatus status
	);

	@Query("""
		select event
		from StoreEvent event
		join fetch event.store store
		where store.active = true
		  and event.status = :status
		  and event.endsAt > :now
		order by event.startsAt asc
		""")
	List<StoreEvent> findUpcomingPublicEvents(
		@Param("status") EventStatus status,
		@Param("now") Instant now,
		Pageable pageable
	);
}
