package com.castfan.dto;

import java.util.List;

public record CastContentView(List<ContentItem> content, List<TierOption> tiers, Long editingContentId) {
	public record ContentItem(
		Long id, String title, String body, String imageUrl, String audience, String minimumTier,
		String status, String statusTone, String publishedLabel, boolean editable, boolean publishable, boolean archivable
	) { }
	public record TierOption(Long id, String name, int rank, boolean active) { }
}
