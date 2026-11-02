package com.castfan.dto;

import java.util.List;

public record PublicCastProfileView(
	Long id,
	String stageName,
	String handle,
	String bio,
	String avatarUrl,
	String initials,
	String storeName,
	long followerCount,
	boolean followed,
	ShiftSummary nextShift,
	List<ContentSummary> content
) {
	public record ShiftSummary(String dateLabel, String timeLabel, boolean workingNow) {
	}

	public record ContentSummary(
		Long id,
		String title,
		String body,
		String imageUrl,
		String audience,
		String publishedLabel,
		boolean accessible
	) { }
}
