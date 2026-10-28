package com.castfan.dto;

import java.util.List;

public record OwnerEventView(
	List<EventItem> events,
	Long editingEventId,
	String editingImageUrl
) {

	public record EventItem(
		Long id,
		String title,
		String description,
		String imageUrl,
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
