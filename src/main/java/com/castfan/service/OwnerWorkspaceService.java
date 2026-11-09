package com.castfan.service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import com.castfan.dto.OwnerWorkspaceView;
import com.castfan.entity.EventStatus;
import com.castfan.entity.ShiftSchedule;
import com.castfan.entity.ShiftStatus;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OwnerWorkspaceService {

	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Bangkok");
	private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM uuuu", Locale.ENGLISH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

	private final UserAccountRepository userAccountRepository;
	private final StoreRepository storeRepository;
	private final StoreCastRepository storeCastRepository;
	private final ShiftScheduleRepository shiftScheduleRepository;
	private final StoreEventRepository storeEventRepository;
	private final ShiftScheduleService shiftScheduleService;

	public OwnerWorkspaceService(
		UserAccountRepository userAccountRepository,
		StoreRepository storeRepository,
		StoreCastRepository storeCastRepository,
		ShiftScheduleRepository shiftScheduleRepository,
		StoreEventRepository storeEventRepository,
		ShiftScheduleService shiftScheduleService
	) {
		this.userAccountRepository = userAccountRepository;
		this.storeRepository = storeRepository;
		this.storeCastRepository = storeCastRepository;
		this.shiftScheduleRepository = shiftScheduleRepository;
		this.storeEventRepository = storeEventRepository;
		this.shiftScheduleService = shiftScheduleService;
	}

	@Transactional(readOnly = true)
	public OwnerWorkspaceView load(String email, LocalDate requestedDate) {
		var account = userAccountRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new DomainRuleViolationException("Owner account not found."));
		var stores = storeRepository.findFirstByOwnerIdAndActiveTrueOrderByIdAsc(account.getId()).stream().toList();
		var zone = stores.stream().findFirst()
			.map(store -> safeZone(store.getTimezone())).orElse(DEFAULT_ZONE);
		var today = LocalDate.now(zone);
		var selectedDate = requestedDate == null ? today : requestedDate;
		if (selectedDate.isBefore(today.minusYears(1)) || selectedDate.isAfter(today.plusYears(2))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Schedule date is outside the supported range");
		}
		var selectedStartsAt = selectedDate.atStartOfDay(zone).toInstant();
		var selectedEndsAt = selectedDate.plusDays(1).atStartOfDay(zone).toInstant();
		var todayStartsAt = today.atStartOfDay(zone).toInstant();
		var todayEndsAt = today.plusDays(1).atStartOfDay(zone).toInstant();
		var assignments = storeCastRepository.findActiveForOwner(email);
		var pending = shiftScheduleRepository.findForOwnerByStatus(
			email, ShiftStatus.SUBMITTED, PageRequest.of(0, 25)
		);
		var daily = shiftScheduleRepository.findForOwnerInRange(email, selectedStartsAt, selectedEndsAt);
		var approvedTodayCount = shiftScheduleRepository.findForOwnerInRange(email, todayStartsAt, todayEndsAt)
			.stream().filter(shift -> shift.getStatus() == ShiftStatus.APPROVED).count();

		return new OwnerWorkspaceView(
			account.getDisplayName(),
			account.getEmail(),
			stores.stream().map(store -> new OwnerWorkspaceView.StoreItem(
				store.getId(), SystemStoreService.DISPLAY_NAME, store.getAddress(), store.getTimezone(), store.isActive()
			)).toList(),
			assignments.stream().map(assignment -> assignment.getCastProfile().getId()).distinct().count(),
			shiftScheduleRepository.countForOwnerByStatus(email, ShiftStatus.SUBMITTED),
			approvedTodayCount,
			storeEventRepository.countUpcomingForOwner(email, EventStatus.PUBLISHED, Instant.now()),
			selectedDate.toString(),
			FULL_DATE.format(selectedDate),
			pending.stream().map(this::toShiftItem).toList(),
			daily.stream().map(this::toShiftItem).toList()
		);
	}

	@Transactional
	public void approve(String email, Long shiftId, String reviewNote) {
		var owner = userAccountRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new DomainRuleViolationException("Owner account not found."));
		shiftScheduleService.approveShift(owner, shiftId, reviewNote);
	}

	@Transactional
	public void reject(String email, Long shiftId, String reviewNote) {
		var owner = userAccountRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new DomainRuleViolationException("Owner account not found."));
		if (reviewNote == null || reviewNote.isBlank()) {
			throw new DomainRuleViolationException("A reason is required when rejecting a shift.");
		}
		shiftScheduleService.rejectShift(owner, shiftId, reviewNote);
	}

	private OwnerWorkspaceView.ShiftItem toShiftItem(ShiftSchedule schedule) {
		var profile = schedule.getStoreCast().getCastProfile();
		var store = schedule.getStoreCast().getStore();
		var zone = safeZone(store.getTimezone());
		var startsAt = schedule.getStartsAt().atZone(zone);
		var endsAt = schedule.getEndsAt().atZone(zone);
		return new OwnerWorkspaceView.ShiftItem(
			schedule.getId(),
			profile.getId(),
			profile.getStageName(),
			initials(profile.getStageName()),
			profile.getAvatarUrl(),
			SystemStoreService.DISPLAY_NAME,
			FULL_DATE.format(startsAt),
			TIME.format(startsAt) + " - " + TIME.format(endsAt),
			statusLabel(schedule.getStatus()),
			schedule.getStatus().name().toLowerCase(Locale.ENGLISH),
			schedule.getReviewNote(),
			schedule.getStatus() == ShiftStatus.SUBMITTED
		);
	}

	private String statusLabel(ShiftStatus status) {
		return switch (status) {
			case SUBMITTED -> "Pending";
			case APPROVED -> "Approved";
			case REJECTED -> "Rejected";
			case CANCELLED -> "Cancelled";
		};
	}

	private ZoneId safeZone(String timezone) {
		try {
			return ZoneId.of(timezone);
		} catch (DateTimeException exception) {
			return DEFAULT_ZONE;
		}
	}

	private String initials(String name) {
		return Arrays.stream(name.trim().split("\\s+"))
			.filter(part -> !part.isEmpty())
			.limit(2)
			.map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH))
			.collect(Collectors.joining());
	}
}
