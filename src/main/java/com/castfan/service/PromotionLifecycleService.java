package com.castfan.service;

import java.time.Instant;

import com.castfan.repository.StorePromotionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromotionLifecycleService {

	private final StorePromotionRepository storePromotionRepository;

	public PromotionLifecycleService(StorePromotionRepository storePromotionRepository) {
		this.storePromotionRepository = storePromotionRepository;
	}

	@Transactional
	public void archiveExpiredPromotions() {
		var expiredPromotions = storePromotionRepository.findExpiredActivePromotions(Instant.now());
		for (var promotion : expiredPromotions) {
			promotion.archive();
		}
		storePromotionRepository.saveAll(expiredPromotions);
	}

	@Scheduled(fixedDelayString = "${castfan.promotions.expiration-check-delay-ms:60000}")
	public void scheduledArchiveExpiredPromotions() {
		archiveExpiredPromotions();
	}
}
