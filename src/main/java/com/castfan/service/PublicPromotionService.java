package com.castfan.service;

import java.time.Instant;
import java.util.List;

import com.castfan.dto.PublicPromotionView;
import com.castfan.repository.StorePromotionRepository;
import com.castfan.entity.PromotionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PublicPromotionService {

    private final StorePromotionRepository storePromotionRepository;
    private final StorePromotionImageService storePromotionImageService;
    private final PromotionLifecycleService promotionLifecycleService;

    public PublicPromotionService(
        StorePromotionRepository storePromotionRepository,
        StorePromotionImageService storePromotionImageService,
        PromotionLifecycleService promotionLifecycleService
    ) {
        this.storePromotionRepository = storePromotionRepository;
        this.storePromotionImageService = storePromotionImageService;
        this.promotionLifecycleService = promotionLifecycleService;
    }

    public List<PublicPromotionView> findCurrentPublicPromotions(Pageable pageable) {
        promotionLifecycleService.archiveExpiredPromotions();
        return storePromotionRepository
            .findCurrentPublicPromotions(Instant.now(), PromotionStatus.ACTIVE, pageable)
            .stream()
            .map(promotion -> new PublicPromotionView(
                promotion,
                storePromotionImageService.publicUrl(promotion)
            ))
            .toList();
    }
}