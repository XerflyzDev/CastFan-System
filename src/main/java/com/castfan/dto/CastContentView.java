package com.castfan.dto;

import java.util.List;

public record CastContentView(List<ContentItem> content, List<StoreTierOption> storeTiers, Long editingContentId,
	String editingImageUrl) {
	public record ContentItem(
		Long id, String title, String body, String imageUrl, String audience, String minimumTier,
		String status, String statusTone, String publishedLabel, boolean editable, boolean publishable, boolean archivable
	) { }
	public record StoreTierOption(int rank, String name, long minimumPoints) { }
}
