package com.castfan.dto;

import java.util.List;

public record OwnerWorkspaceView(
	String displayName,
	String email,
	List<StoreItem> stores,
	long activeCastCount,
	long pendingCount,
	long approvedTodayCount,
	long upcomingEventCount,
	String selectedDate,
	String selectedDateLabel,
	String todayDate,
	String selectedWeekLabel,
	String previousWeekDate,
	String nextWeekDate,
	List<ScheduleDay> days,
	List<ShiftItem> pendingShifts,
	List<ShiftItem> dailyShifts
) {

	public record StoreItem(Long id, String name, String address, String timezone, boolean active) {
	}

	public record ScheduleDay(
		String isoDate,
		String dayLabel,
		String dayNumber,
		boolean selected,
		boolean today,
		int shiftCount
	) {
	}

	public record ShiftItem(
		Long id,
		Long castProfileId,
		String castName,
		String initials,
		String avatarUrl,
		String storeName,
		String dateLabel,
		String timeLabel,
		String status,
		String statusTone,
		String reviewNote,
		boolean reviewable
	) {
	}
}
