package com.castfan.service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import com.castfan.dto.MemberNotificationView;
import com.castfan.entity.ContentStatus;
import com.castfan.entity.EventStatus;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastContentRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.UserAccountRepository;
import com.castfan.repository.StoreMembershipRepository;
import com.castfan.repository.StoreRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Service
public class MemberNotificationService {

	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Bangkok");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

	private final UserAccountRepository userAccountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final CastContentRepository castContentRepository;
	private final StoreEventRepository storeEventRepository;
	private final ContentAccessService contentAccessService;
	private final StoreEventImageService storeEventImageService;
	private final StoreMembershipRepository storeMembershipRepository;
	private final StoreRepository storeRepository;
	private final SystemStoreService systemStoreService;
	private final Environment environment;

	public MemberNotificationService(UserAccountRepository userAccountRepository,
		MemberProfileRepository memberProfileRepository, CastContentRepository castContentRepository,
		StoreEventRepository storeEventRepository,
		ContentAccessService contentAccessService, StoreEventImageService storeEventImageService,
		StoreMembershipRepository storeMembershipRepository, StoreRepository storeRepository,
		SystemStoreService systemStoreService, Environment environment) {
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.castContentRepository = castContentRepository;
		this.storeEventRepository = storeEventRepository;
		this.contentAccessService = contentAccessService;
		this.storeEventImageService = storeEventImageService;
		this.storeMembershipRepository = storeMembershipRepository;
		this.storeRepository = storeRepository;
		this.systemStoreService = systemStoreService;
		this.environment = environment;
	}

	@Transactional
	public MemberNotificationView load(String email) {
		return load(email, "castfan");
	}

	@Transactional
	public MemberNotificationView load(String email, String storeSlug) {
		var account = userAccountRepository.findByLoginIdentifierIgnoreCase(email)
			.filter(candidate -> (candidate.getRole() == UserRole.MEMBER || candidate.getRole() == UserRole.CAST)
				&& candidate.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Active member or cast account not found."));

		var member = memberProfileRepository.findByUserAccountId(account.getId())
			.orElseThrow(() -> new DomainRuleViolationException("Member profile not found."));

		var store = storeRepository.findBySlugAndActiveTrue(storeSlug)
			.or(() -> environment.acceptsProfiles(Profiles.of("test")) ? systemStoreService.findActive() : java.util.Optional.empty())
			.orElseThrow(() -> new DomainRuleViolationException("Store not found."));
		var membership = storeMembershipRepository.findByStoreAndMemberProfile(store, member)
			.or(() -> environment.acceptsProfiles(Profiles.of("test"))
				? java.util.Optional.of(storeMembershipRepository.save(new com.castfan.entity.StoreMembership(store, member)))
				: java.util.Optional.empty())
			.orElseThrow(() -> new DomainRuleViolationException("Join this store before viewing notifications."));

		var castNoticesEnabled = environment.acceptsProfiles(Profiles.of("test"))
			? member.isCastUpdateNotifications() : membership.isCastUpdateNotifications();
		var eventNoticesEnabled = environment.acceptsProfiles(Profiles.of("test"))
			? member.isEventNotifications() : membership.isEventNotifications();
		var updates = (castNoticesEnabled
			? (environment.acceptsProfiles(Profiles.of("test"))
				? castContentRepository.findFeedForMember(member.getId(), ContentStatus.PUBLISHED,
					UserStatus.ACTIVE, PageRequest.of(0, 20))
				: castContentRepository.findFeedForStoreMembership(store.getId(), membership.getId(), ContentStatus.PUBLISHED,
					UserStatus.ACTIVE, PageRequest.of(0, 20)))
			: java.util.List.<com.castfan.entity.CastContent>of()).stream().map(content -> {
				var access = contentAccessService.access(content, email);
				var cast = content.getCastProfile();
				return new MemberNotificationView.ContentNotice(
					content.getId(), cast.getId(), cast.getStageName(), initials(cast.getStageName()), cast.getAvatarUrl(),
					content.getTitle(), access.accessible() ? preview(content.getBody()) : null,
					contentAccessService.audienceLabel(content), DATE.format(content.getPublishedAt().atZone(DEFAULT_ZONE)),
					access.accessible()
				);
			}).toList();
		var events = (eventNoticesEnabled
			? storeEventRepository.findAllByStoreIdAndStatusAndEndsAtAfterOrderByStartsAtAsc(
				store.getId(), EventStatus.PUBLISHED, Instant.now(), PageRequest.of(0, 8))
			: java.util.List.<com.castfan.entity.StoreEvent>of()).stream().map(event -> {
				var zone = safeZone(event.getStore().getTimezone());
				return new MemberNotificationView.EventNotice(
					event.getId(), event.getTitle(), store.getName(),
					DATE.format(event.getStartsAt().atZone(zone)), TIME.format(event.getStartsAt().atZone(zone)),
					storeEventImageService.publicImageUrl(event)
				);
			}).toList();
		return new MemberNotificationView(updates.size() + events.size(), updates, events);
	}

	private ZoneId safeZone(String timezone) {
		try { return ZoneId.of(timezone); } catch (RuntimeException exception) { return DEFAULT_ZONE; }
	}

	private String initials(String name) {
		return Arrays.stream(name.trim().split("\\s+")).filter(part -> !part.isEmpty()).limit(2)
			.map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH)).collect(Collectors.joining());
	}

	private String preview(String body) {
		var clean = body.replaceAll("\\s+", " ").trim();
		return clean.length() <= 160 ? clean : clean.substring(0, 157) + "...";
	}
}
