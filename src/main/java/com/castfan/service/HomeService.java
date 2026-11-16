package com.castfan.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.castfan.dto.HomeViewModel;
import com.castfan.entity.CastContent;
import com.castfan.entity.ContentStatus;
import com.castfan.entity.EventStatus;
import com.castfan.entity.ShiftSchedule;
import com.castfan.entity.ShiftStatus;
import com.castfan.entity.Store;
import com.castfan.entity.StoreMembershipStatus;
import com.castfan.entity.PromotionStatus;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastFollowRepository;
import com.castfan.repository.CastContentRepository;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.StoreMembershipRepository;
import com.castfan.repository.StorePromotionRepository;
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
	private final StorePromotionRepository storePromotionRepository;
	private final StoreMembershipRepository storeMembershipRepository;
	private final UserAccountRepository userAccountRepository;
	private final CastProfileRepository castProfileRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final CastFollowRepository castFollowRepository;
	private final CastContentRepository castContentRepository;
	private final ContentAccessService contentAccessService;
	private final StoreEventImageService storeEventImageService;
	private final StorePromotionImageService storePromotionImageService;
	private final CastContentImageService castContentImageService;

	public HomeService(
		StoreRepository storeRepository,
		ShiftScheduleRepository shiftScheduleRepository,
		StoreEventRepository storeEventRepository,
		StorePromotionRepository storePromotionRepository,
		StoreMembershipRepository storeMembershipRepository,
		UserAccountRepository userAccountRepository,
		CastProfileRepository castProfileRepository,
		MemberProfileRepository memberProfileRepository,
		CastFollowRepository castFollowRepository,
		CastContentRepository castContentRepository,
		ContentAccessService contentAccessService,
		StoreEventImageService storeEventImageService,
		StorePromotionImageService storePromotionImageService,
		CastContentImageService castContentImageService
	) {
		this.storeRepository = storeRepository;
		this.shiftScheduleRepository = shiftScheduleRepository;
		this.storeEventRepository = storeEventRepository;
		this.storePromotionRepository = storePromotionRepository;
		this.storeMembershipRepository = storeMembershipRepository;
		this.userAccountRepository = userAccountRepository;
		this.castProfileRepository = castProfileRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.castFollowRepository = castFollowRepository;
		this.castContentRepository = castContentRepository;
		this.contentAccessService = contentAccessService;
		this.storeEventImageService = storeEventImageService;
		this.storePromotionImageService = storePromotionImageService;
		this.castContentImageService = castContentImageService;
	}

	@Transactional(readOnly = true)
	public HomeViewModel load(String authenticatedEmail) {
		return load(authenticatedEmail, storeRepository.findFirstByActiveTrueOrderByIdAsc().orElse(null), false);
	}

	@Transactional(readOnly = true)
	public HomeViewModel load(String authenticatedEmail, String storeSlug) {
		var store = storeRepository.findBySlugAndActiveTrue(storeSlug)
			.orElseThrow(() -> new DomainRuleViolationException("Store not found."));
		return load(authenticatedEmail, store, true);
	}

	private HomeViewModel load(String authenticatedEmail, Store store, boolean scopedToStore) {
		var now = Instant.now();
		var zone = store == null ? DEFAULT_ZONE : safeZone(store.getTimezone());
		var today = LocalDate.ofInstant(now, zone);
		var dayStartsAt = today.atStartOfDay(zone).toInstant();
		var dayEndsAt = today.plusDays(1).atStartOfDay(zone).toInstant();

		var schedules = store == null
			? List.<ShiftSchedule>of()
			: shiftScheduleRepository.findPublicShiftsInRange(
				store.getId(), ShiftStatus.APPROVED, UserStatus.ACTIVE, dayStartsAt, dayEndsAt
			);
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

		var eventEntities = store == null
			? storeEventRepository.findUpcomingPublicEvents(
				EventStatus.PUBLISHED, now, PageRequest.of(0, 3))
			: storeEventRepository.findAllByStoreIdAndStatusAndEndsAtAfterOrderByStartsAtAsc(
				store.getId(), EventStatus.PUBLISHED, now, PageRequest.of(0, 3));
		var events = eventEntities.stream()
			.map(event -> new HomeViewModel.FeaturedEvent(
				event.getId(),
				event.getTitle(),
				EVENT_DATE_FORMAT.format(event.getStartsAt().atZone(zone)).toUpperCase(Locale.ENGLISH),
				formatRange(event.getStartsAt(), event.getEndsAt(), zone),
				event.getDescription(),
				storeEventImageService.publicImageUrl(event)
			))
			.toList();

		var promotionEntities = store == null
			? storePromotionRepository.findCurrentPublicPromotions(
				now, PromotionStatus.ACTIVE, PageRequest.of(0, 3))
			: storePromotionRepository.findAllByStoreIdAndStatusAndStartsAtLessThanEqualAndEndsAtAfterOrderByStartsAtDesc(
				store.getId(), PromotionStatus.ACTIVE, now, now, PageRequest.of(0, 3));
		var promotions = promotionEntities.stream()
			.map(promotion -> {
				var promotionZone = safeZone(promotion.getStore().getTimezone());
				return new HomeViewModel.FeaturedPromotion(
					promotion.getId(), promotion.getTitle(), promotion.getDescription(), promotion.getCode(),
					storePromotionImageService.publicUrl(promotion), promotion.getStore().getName(),
					"Ends " + EVENT_DATE_FORMAT.format(promotion.getEndsAt().atZone(promotionZone))
				);
			})
			.toList();

		return new HomeViewModel(
			memberName(authenticatedEmail),
			DAY_FORMAT.format(today),
			store == null ? SystemStoreService.DISPLAY_NAME : store.getName(),
			promotions,
			cast,
			events,
			feed(authenticatedEmail, store, scopedToStore)
		);
	}

	private List<HomeViewModel.FeedItem> feed(String authenticatedEmail, Store store, boolean scopedToStore) {
		var member = activeMember(authenticatedEmail);
		if (member.isEmpty()) {
			return List.of();
		}
		var profile = member.get();
		var content = scopedToStore && store != null
			? storeMembershipRepository.findByStoreAndMemberProfile(store, profile)
				.filter(membership -> membership.getStatus() == StoreMembershipStatus.ACTIVE)
				.map(membership -> castContentRepository.findFeedForStoreMembership(
					store.getId(), membership.getId(), ContentStatus.PUBLISHED, UserStatus.ACTIVE,
					PageRequest.of(0, 6)))
				.orElseGet(List::of)
			: castContentRepository.findFeedForMember(
				profile.getId(), ContentStatus.PUBLISHED, UserStatus.ACTIVE, PageRequest.of(0, 6));
		return content.stream().map(item -> {
			var access = contentAccessService.access(item, authenticatedEmail);
			var castProfile = item.getCastProfile();
			return new HomeViewModel.FeedItem(
				item.getId(), castProfile.getId(), castProfile.getStageName(), initials(castProfile.getStageName()),
				castProfile.getAvatarUrl(), item.getTitle(), access.accessible() ? preview(item.getBody()) : null,
				access.accessible() ? castContentImageService.publicUrl(item) : null,
				contentAccessService.audienceLabel(item),
				EVENT_DATE_FORMAT.format(item.getPublishedAt().atZone(DEFAULT_ZONE)), access.accessible()
			);
		}).toList();
	}

	private java.util.Optional<com.castfan.entity.MemberProfile> activeMember(String email) {
		if (email == null) return java.util.Optional.empty();
		return userAccountRepository.findByLoginIdentifierIgnoreCase(email)
			.filter(account -> (account.getRole() == UserRole.MEMBER || account.getRole() == UserRole.CAST)
				&& account.getStatus() == UserStatus.ACTIVE)
			.flatMap(account -> memberProfileRepository.findByUserAccountId(account.getId()));
	}

	private Set<Long> followedIds(String email, java.util.List<Long> profileIds) {
		if (email == null || profileIds.isEmpty()) {
			return Set.of();
		}
		return userAccountRepository.findByLoginIdentifierIgnoreCase(email)
			.filter(account -> (account.getRole() == UserRole.MEMBER || account.getRole() == UserRole.CAST)
				&& account.getStatus() == UserStatus.ACTIVE)
			.flatMap(account -> memberProfileRepository.findByUserAccountId(account.getId()))
			.map(profile -> Set.copyOf(castFollowRepository.findFollowedCastProfileIds(profile.getId(), profileIds)))
			.orElseGet(Set::of);
	}

	private String memberName(String email) {
		if (email == null) {
			return "there";
		}
		return userAccountRepository.findByLoginIdentifierIgnoreCase(email)
			.filter(account -> account.getStatus() == UserStatus.ACTIVE)
			.map(account -> {
				if (account.getRole() == UserRole.CAST) {
					return castProfileRepository.findByUserAccountId(account.getId())
						.map(profile -> profile.getStageName())
						.filter(name -> name != null && !name.isBlank())
						.orElse(account.getDisplayName());
				}
				return account.getDisplayName();
			})
			.filter(name -> name != null && !name.isBlank())
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

	private String preview(String body) {
		var clean = body.replaceAll("\\s+", " ").trim();
		return clean.length() <= 140 ? clean : clean.substring(0, 137) + "...";
	}
}
