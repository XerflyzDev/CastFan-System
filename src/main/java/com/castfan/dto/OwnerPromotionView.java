package com.castfan.dto;

import java.util.List;

public record OwnerPromotionView(List<StoreOption> stores, List<PromotionItem> promotions, Long editingPromotionId) {
	public record StoreOption(Long id, String name, String timezone) { }
	public record PromotionItem(Long id, String title, String description, String code, String bannerUrl,
		String storeName, String rangeLabel, String status, String statusTone, boolean editable,
		boolean activatable, boolean deactivatable) { }
}
