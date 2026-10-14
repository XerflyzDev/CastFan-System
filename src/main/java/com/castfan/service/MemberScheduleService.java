package com.castfan.service;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.castfan.dto.MemberScheduleView;
import com.castfan.entity.EventStatus;
import com.castfan.entity.ShiftStatus;
import com.castfan.entity.UserStatus;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.StoreRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemberScheduleService {

	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Bangkok");
	private static final DateTimeFormatter DAY_NAME = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
	private static final DateTimeFormatter DAY_NUMBER = DateTimeFormatter.ofPattern("d", Locale.ENGLISH);
	private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH);
	private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
	private static final DateTimeFormatter EVENT_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
	private static final String[] AVATAR_TONES = {"rose", "lavender", "amber"};

	private final StoreRepository storeRepository;
	private final ShiftScheduleRepository shiftScheduleRepository;
	private final StoreEventRepository storeEventRepository;

	public MemberScheduleService(
		StoreRepository storeRepository,
		ShiftScheduleRepository shiftScheduleRepository,
		StoreEventRepository storeEventRepository
	) {
		this.storeRepository = storeRepository;
		this.shiftScheduleRepository = shiftScheduleRepository;
		this.storeEventRepository = storeEventRepository;
	}

	@Transactional(readOnly = true)
	public MemberScheduleView load(LocalDate requestedDate) {
		var now = Instant.now();
		var store = storeRepository.findFirstByActiveTrueOrderByIdAsc();
		var zone = store.map(value -> safeZone(value.getTimezone())).orElse(DEFAULT_ZONE);
		var today = LocalDate.ofInstant(now, zone);
		if (requestedDate != null
			&& (requestedDate.isBefore(today.minusYears(1)) || requestedDate.isAfter(today.plusYears(2)))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule date is outside the supported range");
		}
		var selectedDate = requestedDate == null ? today : requestedDate;
		var dayStartsAt = selectedDate.atStartOfDay(zone).toInstant();
		var dayEndsAt = selectedDate.plusDays(1).atStartOfDay(zone).toInstant();

		var schedules = store.map(value -> shiftScheduleRepository.findPublicShiftsInRange(
			value.getId(), ShiftStatus.APPROVED, UserStatus.ACTIVE, dayStartsAt, dayEndsAt
		)).orElseGet(List::of);
		var shifts = schedules.stream().map(schedule -> {
			var profile = schedule.getStoreCast().getCastProfile();
			var startsSoon = !now.isAfter(schedule.getStartsAt())
				&& now.plusSeconds(2 * 60 * 60).isAfter(schedule.getStartsAt());
			return new MemberScheduleView.ShiftItem(
				schedule.getId(),
				profile.getId(),
				profile.getStageName(),
				initials(profile.getStageName()),
				avatarTone(profile.getId()),
				profile.getAvatarUrl(),
				TIME.format(schedule.getStartsAt().atZone(zone)),
				formatRange(schedule.getStartsAt(), schedule.getEndsAt(), zone),
				!now.isBefore(schedule.getStartsAt()) && now.isBefore(schedule.getEndsAt()),
				startsSoon
			);
		}).toList();

		var weekStartsOn = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
		var days = IntStream.range(0, 7)
			.mapToObj(weekStartsOn::plusDays)
			.map(date -> new MemberScheduleView.ScheduleDay(
				date.toString(), DAY_NAME.format(date), DAY_NUMBER.format(date),
				date.equals(selectedDate), date.equals(today)
			))
			.toList();

		var upcomingEvent = storeEventRepository.findUpcomingPublicEvents(
			EventStatus.PUBLISHED, dayStartsAt, PageRequest.of(0, 1)
		).stream().findFirst().map(event -> new MemberScheduleView.UpcomingEvent(
			event.getId(), event.getTitle(), EVENT_DATE.format(event.getStartsAt().atZone(zone))
		)).orElse(null);

		var closingTime = schedules.stream()
			.map(schedule -> schedule.getEndsAt().atZone(zone).toLocalTime())
			.max(java.time.LocalTime::compareTo)
			.map(TIME::format)
			.orElse(null);

		return new MemberScheduleView(
			store.map(value -> value.getName()).orElse("CastFan"),
			selectedDate.toString(),
			FULL_DATE.format(selectedDate),
			MONTH.format(selectedDate),
			closingTime,
			days,
			shifts,
			upcomingEvent
		);
	}

	private ZoneId safeZone(String timezone) {
		try {
			return ZoneId.of(timezone);
		} catch (DateTimeException exception) {
			return DEFAULT_ZONE;
		}
	}

	private String formatRange(Instant startsAt, Instant endsAt, ZoneId zone) {
		return TIME.format(startsAt.atZone(zone)) + " - " + TIME.format(endsAt.atZone(zone));
	}

	private String avatarTone(Long profileId) {
		return AVATAR_TONES[Math.floorMod(profileId.hashCode(), AVATAR_TONES.length)];
	}

	private String initials(String name) {
		return Arrays.stream(name.trim().split("\\s+"))
			.filter(part -> !part.isEmpty())
			.limit(2)
			.map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH))
			.collect(Collectors.joining());
	}
}
