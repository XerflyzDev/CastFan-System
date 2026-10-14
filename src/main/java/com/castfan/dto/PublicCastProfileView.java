package com.castfan.dto;

import java.math.BigDecimal;
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
	List<TierSummary> tiers,
	List<ContentSummary> content
) {
	public record ShiftSummary(String dateLabel, String timeLabel, boolean workingNow) {
	}

	public record TierSummary(
		Long id,
		String name,
		String description,
		BigDecimal monthlyPrice,
		String currency
	) {
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
