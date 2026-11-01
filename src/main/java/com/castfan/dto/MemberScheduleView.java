package com.castfan.dto;

import java.util.List;

public record MemberScheduleView(
	String storeName,
	String selectedDate,
	String selectedDateLabel,
	String monthLabel,
	String closingTime,
	int visibleShiftCount,
	boolean weekView,
	String previousWeekDate,
	String nextWeekDate,
	List<ScheduleDay> days,
	List<ShiftItem> shifts,
	List<ScheduleGroup> weekSchedule,
	UpcomingEvent upcomingEvent
) {

	public record ScheduleDay(
		String isoDate,
		String dayLabel,
		String dayNumber,
		boolean selected,
		boolean today,
		int shiftCount
	) {
	}

	public record ScheduleGroup(String isoDate, String dateLabel, boolean today, List<ShiftItem> shifts) { }

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
