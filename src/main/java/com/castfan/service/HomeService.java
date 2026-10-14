package com.castfan.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.castfan.dto.HomeViewModel;
import com.castfan.entity.EventStatus;
import com.castfan.entity.ShiftStatus;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastFollowRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {

	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Bangkok");
	private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH);
	private static final DateTimeFormatter EVENT_DATE_FORMAT = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
	private static final String[] AVATAR_TONES = {"rose", "lavender", "amber"};

	private final StoreRepository storeRepository;
	private final ShiftScheduleRepository shiftScheduleRepository;
	private final StoreEventRepository storeEventRepository;
	private final UserAccountRepository userAccountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final CastFollowRepository castFollowRepository;

	public HomeService(
		StoreRepository storeRepository,
		ShiftScheduleRepository shiftScheduleRepository,
		StoreEventRepository storeEventRepository,
		UserAccountRepository userAccountRepository,
		MemberProfileRepository memberProfileRepository,
		CastFollowRepository castFollowRepository
	) {
		this.storeRepository = storeRepository;
		this.shiftScheduleRepository = shiftScheduleRepository;
		this.storeEventRepository = storeEventRepository;
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.castFollowRepository = castFollowRepository;
	}

	@Transactional(readOnly = true)
	public HomeViewModel load(String authenticatedEmail) {
		var now = Instant.now();
		var store = storeRepository.findFirstByActiveTrueOrderByIdAsc();
		var zone = store.map(value -> safeZone(value.getTimezone())).orElse(DEFAULT_ZONE);
		var today = LocalDate.ofInstant(now, zone);
		var dayStartsAt = today.atStartOfDay(zone).toInstant();
		var dayEndsAt = today.plusDays(1).atStartOfDay(zone).toInstant();

		var schedules = store.map(value -> shiftScheduleRepository.findPublicShiftsInRange(
			value.getId(), ShiftStatus.APPROVED, UserStatus.ACTIVE, dayStartsAt, dayEndsAt
		)).orElseGet(java.util.List::of);
		var profileIds = schedules.stream()
			.map(schedule -> schedule.getStoreCast().getCastProfile().getId())
			.distinct()
			.toList();
		var followedIds = followedIds(authenticatedEmail, profileIds);
		var cast = schedules.stream()
			.map(schedule -> {
				var profile = schedule.getStoreCast().getCastProfile();
				return new HomeViewModel.CastSummary(
					profile.getId(),
					profile.getStageName(),
					initials(profile.getStageName()),
					formatRange(schedule.getStartsAt(), schedule.getEndsAt(), zone),
					avatarTone(profile.getId()),
					profile.getAvatarUrl(),
					!now.isBefore(schedule.getStartsAt()) && now.isBefore(schedule.getEndsAt()),
					followedIds.contains(profile.getId())
				);
			})
			.toList();

		var events = storeEventRepository.findUpcomingPublicEvents(
			EventStatus.PUBLISHED,
			now,
			PageRequest.of(0, 3)
		).stream()
			.map(event -> new HomeViewModel.FeaturedEvent(
				event.getId(),
				event.getTitle(),
				EVENT_DATE_FORMAT.format(event.getStartsAt().atZone(zone)).toUpperCase(Locale.ENGLISH),
				formatRange(event.getStartsAt(), event.getEndsAt(), zone),
				event.getDescription(),
				event.getImageUrl()
			))
			.toList();

		return new HomeViewModel(
			memberName(authenticatedEmail),
			DAY_FORMAT.format(today),
			store.map(value -> value.getName()).orElse("CastFan"),
			cast,
			events
		);
	}

	private Set<Long> followedIds(String email, java.util.List<Long> profileIds) {
		if (email == null || profileIds.isEmpty()) {
			return Set.of();
		}
		return userAccountRepository.findByEmailIgnoreCase(email)
			.filter(account -> account.getRole() == UserRole.MEMBER && account.getStatus() == UserStatus.ACTIVE)
			.flatMap(account -> memberProfileRepository.findByUserAccountId(account.getId()))
			.map(profile -> Set.copyOf(castFollowRepository.findFollowedCastProfileIds(profile.getId(), profileIds)))
			.orElseGet(Set::of);
	}

	private String memberName(String email) {
		if (email == null) {
			return "there";
		}
		return userAccountRepository.findByEmailIgnoreCase(email)
			.map(account -> account.getDisplayName())
			.filter(name -> !name.isBlank())
			.orElse("there");
	}

	private ZoneId safeZone(String timezone) {
		try {
			return ZoneId.of(timezone);
		} catch (RuntimeException exception) {
			return DEFAULT_ZONE;
		}
	}

	private String formatRange(Instant startsAt, Instant endsAt, ZoneId zone) {
		return TIME_FORMAT.format(startsAt.atZone(zone)) + " - " + TIME_FORMAT.format(endsAt.atZone(zone));
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
