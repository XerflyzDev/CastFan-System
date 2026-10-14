package com.castfan.dto;

import java.util.List;

public record HomeViewModel(
	String memberName,
	String dateLabel,
	String storeName,
	List<CastSummary> workingToday,
	List<FeaturedEvent> featuredEvents
) {

	public record CastSummary(
		Long id,
		String name,
		String initials,
		String shiftTime,
		String avatarTone,
		String avatarUrl,
		boolean workingNow,
		boolean followed
	) {
	}

	public record FeaturedEvent(
		Long id,
		String title,
		String date,
		String time,
		String description,
		String imageUrl
	) {
	}
}
