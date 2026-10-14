package com.castfan.dto;

import java.util.List;

public record OwnerStoreView(
	List<StoreItem> stores,
	List<CastOption> availableCast,
	List<AssignmentItem> assignments,
	Long editingStoreId
) {
	public record StoreItem(
		Long id, String name, String slug, String description, String address, String timezone,
		boolean active, long activeCastCount
	) { }

	public record CastOption(Long id, String stageName, String email) { }

	public record AssignmentItem(
		Long id, Long storeId, String storeName, Long castProfileId, String castName,
		String email, String initials, String avatarUrl, boolean active
	) { }
}
