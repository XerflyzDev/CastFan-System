package com.castfan.dto;

import java.util.List;

public record MemberNotificationView(
	int totalCount,
	List<ContentNotice> contentUpdates,
	List<EventNotice> upcomingEvents
) {
	public record ContentNotice(
		Long contentId,
		Long castProfileId,
		String castName,
		String castInitials,
		String castAvatarUrl,
		String title,
		String bodyPreview,
		String audience,
		String publishedLabel,
		boolean accessible
	) { }

	public record EventNotice(
		Long eventId,
		String title,
		String storeName,
		String dateLabel,
		String timeLabel,
		String imageUrl
	) { }
}
