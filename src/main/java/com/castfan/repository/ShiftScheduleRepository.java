package com.castfan.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.castfan.entity.ShiftSchedule;
import com.castfan.entity.ShiftStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftScheduleRepository extends JpaRepository<ShiftSchedule, Long> {

	List<ShiftSchedule> findAllByStoreCastIdOrderByStartsAtAsc(Long storeCastId);

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
}
