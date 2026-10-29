package com.castfan.service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import com.castfan.dto.CastProfileForm;
import com.castfan.dto.CastShiftForm;
import com.castfan.dto.CastWorkspaceView;
import com.castfan.entity.ShiftStatus;
import com.castfan.repository.CastFollowRepository;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberTierRepository;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CastWorkspaceService {

	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Bangkok");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

	private final UserAccountRepository userAccountRepository;
	private final CastProfileRepository castProfileRepository;
	private final StoreCastRepository storeCastRepository;
	private final ShiftScheduleRepository shiftScheduleRepository;
	private final CastFollowRepository castFollowRepository;
	private final MemberTierRepository memberTierRepository;
	private final ShiftScheduleService shiftScheduleService;
	private final ProfileService profileService;
	private final SystemStoreService systemStoreService;

	public CastWorkspaceService(
		UserAccountRepository userAccountRepository,
		CastProfileRepository castProfileRepository,
		StoreCastRepository storeCastRepository,
		ShiftScheduleRepository shiftScheduleRepository,
		CastFollowRepository castFollowRepository,
		MemberTierRepository memberTierRepository,
		ShiftScheduleService shiftScheduleService,
		ProfileService profileService,
		SystemStoreService systemStoreService
	) {
		this.userAccountRepository = userAccountRepository;
		this.castProfileRepository = castProfileRepository;
		this.storeCastRepository = storeCastRepository;
		this.shiftScheduleRepository = shiftScheduleRepository;
		this.castFollowRepository = castFollowRepository;
		this.memberTierRepository = memberTierRepository;
		this.shiftScheduleService = shiftScheduleService;
		this.profileService = profileService;
		this.systemStoreService = systemStoreService;
	}

	@Transactional(readOnly = true)
	public CastWorkspaceView load(String email, int shiftLimit) {
		var profile = requireProfile(email);
		var systemStore = systemStoreService.findActive();
		var assignments = storeCastRepository.findAllByCastProfileIdAndActiveTrue(profile.getId()).stream()
			.filter(assignment -> systemStore.map(store -> assignment.getStore().getId().equals(store.getId())).orElse(false))
			.map(assignment -> new CastWorkspaceView.Assignment(
				assignment.getId(), SystemStoreService.DISPLAY_NAME, assignment.getStore().getTimezone()
			))
			.toList();
		var now = Instant.now();
		var schedules = shiftScheduleRepository.findUpcomingForCast(
			email, now.minusSeconds(12 * 60 * 60), PageRequest.of(0, Math.max(1, shiftLimit))
		).stream().filter(schedule -> systemStore.map(store -> schedule.getStoreCast().getStore().getId().equals(store.getId())).orElse(false)).toList();
		var shifts = schedules.stream().map(schedule -> {
			var zone = safeZone(schedule.getStoreCast().getStore().getTimezone());
			var startsAt = schedule.getStartsAt().atZone(zone);
			var endsAt = schedule.getEndsAt().atZone(zone);
			return new CastWorkspaceView.ShiftItem(
				schedule.getId(),
				SystemStoreService.DISPLAY_NAME,
				DATE.format(startsAt),
				DAY.format(startsAt),
				TIME.format(startsAt) + " - " + TIME.format(endsAt),
				statusLabel(schedule.getStatus()),
				schedule.getStatus().name().toLowerCase(Locale.ENGLISH),
				schedule.getStatus() == ShiftStatus.SUBMITTED || schedule.getStatus() == ShiftStatus.APPROVED,
				!now.isBefore(schedule.getStartsAt()) && now.isBefore(schedule.getEndsAt())
			);
		}).toList();
		var tiers = memberTierRepository.findAllByCastProfileIdOrderByRankAsc(profile.getId());
		var pendingCount = schedules.stream().filter(shift -> shift.getStatus() == ShiftStatus.SUBMITTED).count();
		var approvedCount = schedules.stream().filter(shift -> shift.getStatus() == ShiftStatus.APPROVED).count();
		var nextShiftLabel = schedules.stream()
			.filter(shift -> shift.getStatus() == ShiftStatus.APPROVED)
			.findFirst()
			.map(shift -> formatNextShift(shift.getStartsAt(), shift.getStoreCast().getStore().getTimezone()))
			.orElse("No approved shift");

		return new CastWorkspaceView(
			new CastWorkspaceView.Profile(
				profile.getId(), profile.getStageName(), initials(profile.getStageName()), profile.getBio(),
				profile.getAvatarUrl(), profile.isDiscoverable(), castFollowRepository.countByCastProfileId(profile.getId()),
				tiers.stream().filter(tier -> tier.isActive()).count(), profile.getUserAccount().getEmail()
			),
			assignments,
			shifts,
			pendingCount,
			approvedCount,
			nextShiftLabel
		);
	}

	@Transactional(readOnly = true)
	public CastProfileForm profileForm(String email) {
		var profile = requireProfile(email);
		var form = new CastProfileForm();
		form.setStageName(profile.getStageName());
		form.setBio(profile.getBio());
		form.setDiscoverable(profile.isDiscoverable());
		return form;
	}

	@Transactional
	public void submitShift(String email, CastShiftForm form) {
		var account = userAccountRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new DomainRuleViolationException("Cast account not found."));
		var profile = requireProfile(email);
		var systemStore = systemStoreService.requireActive();
		var assignment = storeCastRepository.findAllByCastProfileIdAndActiveTrue(profile.getId()).stream()
			.filter(value -> value.getStore().getId().equals(systemStore.getId()))
			.findFirst()
			.orElseThrow(() -> new DomainRuleViolationException("Active CastFan roster assignment not found."));
		var zone = safeZone(assignment.getStore().getTimezone());
		var today = LocalDate.now(zone);
		if (form.getDate().isBefore(today) || form.getDate().isAfter(today.plusYears(1))) {
			throw new DomainRuleViolationException("Shift date must be between today and one year from now.");
		}
		if (form.getEndsAt().equals(form.getStartsAt())) {
			throw new DomainRuleViolationException("Shift end time must be different from its start time.");
		}
		var startsAt = form.getDate().atTime(form.getStartsAt()).atZone(zone).toInstant();
		var endDate = form.getEndsAt().isAfter(form.getStartsAt()) ? form.getDate() : form.getDate().plusDays(1);
		var endsAt = endDate.atTime(form.getEndsAt()).atZone(zone).toInstant();
		if (!startsAt.isAfter(Instant.now())) {
			throw new DomainRuleViolationException("Shift start time must be in the future.");
		}
		shiftScheduleService.submitShift(account, assignment.getId(), startsAt, endsAt);
	}

	@Transactional
	public void cancelShift(String email, Long shiftId) {
		var account = userAccountRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new DomainRuleViolationException("Cast account not found."));
		shiftScheduleService.cancelShift(account, shiftId);
	}

	@Transactional
	public void updateProfile(String email, CastProfileForm form, MultipartFile image) {
		profileService.updateCastProfile(
			email, form.getStageName(), form.getBio(), form.isDiscoverable(), image, form.isRemoveImage()
		);
	}

	private com.castfan.entity.CastProfile requireProfile(String email) {
		return castProfileRepository.findByUserAccountEmailIgnoreCase(email)
			.orElseThrow(() -> new DomainRuleViolationException("Cast profile not found."));
	}

	private String formatNextShift(Instant startsAt, String timezone) {
		var value = startsAt.atZone(safeZone(timezone));
		return DATE.format(value) + " at " + TIME.format(value);
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
