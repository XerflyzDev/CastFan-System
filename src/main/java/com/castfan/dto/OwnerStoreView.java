package com.castfan.dto;

import java.util.List;

public record OwnerStoreView(
	List<CastOption> availableCast,
	List<AssignmentItem> assignments
) {
	public record CastOption(Long id, String stageName, String email) { }

	public record AssignmentItem(
		Long id, Long castProfileId, String castName,
		String email, String initials, String avatarUrl, boolean active
	) { }
}
