package com.castfan.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.castfan.entity.ShiftSchedule;
import com.castfan.entity.ShiftStatus;
import com.castfan.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface ShiftScheduleRepository extends JpaRepository<ShiftSchedule, Long> {

	List<ShiftSchedule> findAllByStoreCastIdOrderByStartsAtAsc(Long storeCastId);

	@Query("""
		select schedule
		from ShiftSchedule schedule
		join fetch schedule.storeCast assignment
		join fetch assignment.store store
		join fetch assignment.castProfile profile
		join fetch profile.userAccount account
		where lower(account.email) = lower(:email)
		  and schedule.endsAt > :from
		order by schedule.startsAt asc
		""")
	List<ShiftSchedule> findUpcomingForCast(
		@Param("email") String email,
		@Param("from") Instant from,
		Pageable pageable
	);

	@Query("""
		select schedule
		from ShiftSchedule schedule
		join fetch schedule.storeCast assignment
		join fetch assignment.store store
		join fetch assignment.castProfile profile
		join fetch profile.userAccount account
		where lower(store.owner.email) = lower(:email)
		  and schedule.status = :status
		order by schedule.startsAt asc
		""")
	List<ShiftSchedule> findForOwnerByStatus(
		@Param("email") String email,
		@Param("status") ShiftStatus status,
		Pageable pageable
	);

	@Query("""
		select count(schedule)
		from ShiftSchedule schedule
		where lower(schedule.storeCast.store.owner.email) = lower(:email)
		  and schedule.status = :status
		""")
	long countForOwnerByStatus(
		@Param("email") String email,
		@Param("status") ShiftStatus status
	);

	@Query("""
		select schedule
		from ShiftSchedule schedule
		join fetch schedule.storeCast assignment
		join fetch assignment.store store
		join fetch assignment.castProfile profile
		join fetch profile.userAccount account
		where lower(store.owner.email) = lower(:email)
		  and schedule.startsAt < :endsAt
		  and schedule.endsAt > :startsAt
		order by schedule.startsAt asc, profile.stageName asc
		""")
	List<ShiftSchedule> findForOwnerInRange(
		@Param("email") String email,
		@Param("startsAt") Instant startsAt,
		@Param("endsAt") Instant endsAt
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select schedule from ShiftSchedule schedule where schedule.id = :id")
	Optional<ShiftSchedule> findByIdForUpdate(@Param("id") Long id);

	@Query("""
		select (count(schedule) > 0)
		from ShiftSchedule schedule
		where schedule.storeCast.id = :storeCastId
		  and schedule.status = :status
		  and schedule.startsAt < :endsAt
		  and schedule.endsAt > :startsAt
		""")
	boolean existsOverlap(
		@Param("storeCastId") Long storeCastId,
		@Param("status") ShiftStatus status,
		@Param("startsAt") Instant startsAt,
		@Param("endsAt") Instant endsAt
	);

	@Query("""
		select schedule
		from ShiftSchedule schedule
		join fetch schedule.storeCast assignment
		join fetch assignment.store store
		join fetch assignment.castProfile profile
		join fetch profile.userAccount account
		where schedule.status = :status
		  and store.id = :storeId
		  and schedule.startsAt < :endsAt
		  and schedule.endsAt > :startsAt
		  and assignment.active = true
		  and store.active = true
		  and profile.discoverable = true
		  and account.status = :userStatus
		order by schedule.startsAt asc, profile.stageName asc
		""")
	List<ShiftSchedule> findPublicShiftsInRange(
		@Param("storeId") Long storeId,
		@Param("status") ShiftStatus status,
		@Param("userStatus") UserStatus userStatus,
		@Param("startsAt") Instant startsAt,
		@Param("endsAt") Instant endsAt
	);

	@Query("""
		select schedule
		from ShiftSchedule schedule
		join fetch schedule.storeCast assignment
		join fetch assignment.store store
		join fetch assignment.castProfile profile
		join fetch profile.userAccount account
		where schedule.status = :status
		  and store.id = :storeId
		  and schedule.startsAt < :endsAt
		  and schedule.endsAt > :startsAt
		  and assignment.active = true
		  and store.active = true
		  and account.status = :userStatus
		order by schedule.startsAt asc, profile.stageName asc
		""")
	List<ShiftSchedule> findMemberScheduleShiftsInRange(
		@Param("storeId") Long storeId,
		@Param("status") ShiftStatus status,
		@Param("userStatus") UserStatus userStatus,
		@Param("startsAt") Instant startsAt,
		@Param("endsAt") Instant endsAt
	);

	@Query("""
		select schedule
		from ShiftSchedule schedule
		join fetch schedule.storeCast assignment
		join fetch assignment.store store
		join fetch assignment.castProfile profile
		join fetch profile.userAccount account
		where profile.id = :castProfileId
		  and schedule.status = :status
		  and schedule.endsAt > :now
		  and schedule.startsAt < :horizon
		  and assignment.active = true
		  and store.active = true
		  and profile.discoverable = true
		  and account.status = :userStatus
		order by schedule.startsAt asc
		""")
	List<ShiftSchedule> findUpcomingPublicShiftsForCast(
		@Param("castProfileId") Long castProfileId,
		@Param("status") ShiftStatus status,
		@Param("userStatus") UserStatus userStatus,
		@Param("now") Instant now,
		@Param("horizon") Instant horizon,
		Pageable pageable
	);
}
