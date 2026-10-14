package com.castfan.dto;

import java.util.List;

public record OwnerEventView(
	List<StoreOption> stores,
	List<EventItem> events,
	Long editingEventId
) {

	public record StoreOption(Long id, String name, String timezone) {
	}

	public record EventItem(
		Long id,
		String title,
		String description,
		String imageUrl,
		String storeName,
		String dateLabel,
		String timeLabel,
		String status,
		String statusTone,
		boolean editable,
		boolean publishable,
		boolean archivable,
		boolean publicVisible
	) {
	}
}
