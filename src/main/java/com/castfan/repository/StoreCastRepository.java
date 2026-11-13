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

	Optional<StoreCast> findByStoreIdAndCastProfileId(Long storeId, Long castProfileId);

	List<StoreCast> findAllByCastProfileIdAndActiveTrue(Long castProfileId);

	List<StoreCast> findAllByStoreIdAndActiveTrueOrderByCastProfileStageNameAsc(Long storeId);

	@Query("""
		select assignment
		from StoreCast assignment
		join fetch assignment.store store
		join fetch assignment.castProfile profile
		join fetch profile.userAccount account
		where lower(store.owner.email) = lower(:email)
		  and assignment.active = true
		  and store.active = true
		order by profile.stageName asc
		""")
	List<StoreCast> findActiveForOwner(@Param("email") String email);

	@Query("""
		select assignment
		from StoreCast assignment
		join fetch assignment.store store
		join fetch assignment.castProfile profile
		join fetch profile.userAccount account
		where lower(store.owner.email) = lower(:email)
		order by store.name asc, profile.stageName asc
		""")
	List<StoreCast> findAllForOwner(@Param("email") String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select assignment from StoreCast assignment where assignment.id = :id")
	Optional<StoreCast> findByIdForUpdate(@Param("id") Long id);
}
