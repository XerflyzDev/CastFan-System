package com.castfan.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.castfan.entity.StorePromotion;
import com.castfan.entity.PromotionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StorePromotionRepository extends JpaRepository<StorePromotion, Long> {

	@Query("""
		select promotion from StorePromotion promotion
		join fetch promotion.store store
		where lower(store.owner.email) = lower(:email)
		order by promotion.startsAt desc
		""")
	List<StorePromotion> findAllForOwner(@Param("email") String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		select promotion from StorePromotion promotion
		join fetch promotion.store store
		where promotion.id = :promotionId
		""")
	Optional<StorePromotion> findByIdForUpdate(@Param("promotionId") Long promotionId);

	@Query("""
		select promotion from StorePromotion promotion
		join fetch promotion.store store
		where promotion.status = com.castfan.entity.PromotionStatus.ACTIVE
		  and promotion.endsAt <= :now
		""")
	List<StorePromotion> findExpiredActivePromotions(@Param("now") Instant now);

	boolean existsByCodeIgnoreCase(String code);

	boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

	@Query("""
		select promotion from StorePromotion promotion
		join fetch promotion.store store
		where store.active = true
		  and promotion.status = :status
		  and promotion.startsAt <= :now
		  and promotion.endsAt > :now
		order by promotion.startsAt desc
		""")
	List<StorePromotion> findCurrentPublicPromotions(@Param("now") Instant now,
		@Param("status") PromotionStatus status, Pageable pageable);
}
