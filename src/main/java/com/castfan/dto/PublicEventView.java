package com.castfan.dto;

import java.util.List;

public record PublicEventView(
	Long id,
	String title,
	String description,
	String imageUrl,
	String dateLabel,
	String timeLabel,
	String storeName,
	String location,
	List<CastSummary> featuredCast
) {
	public record CastSummary(
		Long id,
		String name,
		String initials,
		String avatarUrl,
		String shiftTime
	) {
	}
}
