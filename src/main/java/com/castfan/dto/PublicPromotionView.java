package com.castfan.dto;

import com.castfan.entity.StorePromotion;
import com.castfan.service.SystemStoreService;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PublicPromotionView {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Bangkok");

    private static final DateTimeFormatter ENDS_FORMAT =
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private Long id;
    private String title;
    private String description;
    private String code;
    private String bannerUrl;
    private String storeName;
    private String endsLabel;

    public PublicPromotionView() {
    }

    public PublicPromotionView(StorePromotion promotion, String bannerUrl) {
        this.id = promotion.getId();
        this.title = promotion.getTitle();
        this.description = promotion.getDescription();
        this.code = promotion.getCode();
        this.bannerUrl = bannerUrl;
        this.storeName = SystemStoreService.DISPLAY_NAME;

        var zone = safeZone(promotion.getStore().getTimezone());
        this.endsLabel = "Ends " + promotion.getEndsAt()
            .atZone(zone)
            .format(ENDS_FORMAT);
    }

    private ZoneId safeZone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            return DEFAULT_ZONE;
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    public String getBannerUrl() {
        return bannerUrl;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getEndsLabel() {
        return endsLabel;
    }
}