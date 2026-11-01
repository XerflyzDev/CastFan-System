package com.castfan.dto;

import java.util.List;

public record CastWorkspaceView(
	Profile profile,
	List<Assignment> assignments,
	List<ShiftItem> shifts,
	long pendingCount,
	long approvedCount,
	String nextShiftLabel
) {

	public record Profile(
		Long id,
		String stageName,
		String initials,
		String bio,
		String avatarUrl,
		long followerCount,
		long activeTierCount,
		String email
	) {
	}

	public record Assignment(Long id, String storeName, String timezone) {
	}

	public record ShiftItem(
		Long id,
		String storeName,
		String dateLabel,
		String dayLabel,
		String timeLabel,
		String status,
		String statusTone,
		boolean cancellable,
		boolean workingNow
	) {
	}
}
