package com.castfan.dto;

import java.util.List;

public record MemberScheduleView(
	String storeName,
	String selectedDate,
	String selectedDateLabel,
	String monthLabel,
	String closingTime,
	List<ScheduleDay> days,
	List<ShiftItem> shifts,
	UpcomingEvent upcomingEvent
) {

	public record ScheduleDay(
		String isoDate,
		String dayLabel,
		String dayNumber,
		boolean selected,
		boolean today
	) {
	}

	public record ShiftItem(
		Long id,
		Long castProfileId,
		String name,
		String initials,
		String avatarTone,
		String avatarUrl,
		String startsAt,
		String shiftTime,
		boolean workingNow,
		boolean startingSoon
	) {
	}

	public record UpcomingEvent(Long id, String title, String dateLabel) {
	}
}
