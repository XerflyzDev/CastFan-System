package com.castfan.repository;

import java.util.List;
import java.util.Optional;

import com.castfan.entity.StoreCast;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreCastRepository extends JpaRepository<StoreCast, Long> {

	boolean existsByStoreIdAndCastProfileId(Long storeId, Long castProfileId);

	List<StoreCast> findAllByCastProfileIdAndActiveTrue(Long castProfileId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select assignment from StoreCast assignment where assignment.id = :id")
	Optional<StoreCast> findByIdForUpdate(@Param("id") Long id);
}
