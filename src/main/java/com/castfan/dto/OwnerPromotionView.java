package com.castfan.dto;

import java.util.List;

public record OwnerPromotionView(List<PromotionItem> promotions, Long editingPromotionId,
	String editingImageUrl) {
	public record PromotionItem(Long id, String title, String description, String code, String bannerUrl,
		String rangeLabel, String status, String statusTone, boolean editable,
		boolean publishable, boolean archivable) { }
}
