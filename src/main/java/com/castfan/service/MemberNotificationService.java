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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	public MemberNotificationService(UserAccountRepository userAccountRepository,
		MemberProfileRepository memberProfileRepository, CastContentRepository castContentRepository,
		StoreEventRepository storeEventRepository, ContentAccessService contentAccessService,
		StoreEventImageService storeEventImageService) {
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.castContentRepository = castContentRepository;
		this.storeEventRepository = storeEventRepository;
		this.contentAccessService = contentAccessService;
		this.storeEventImageService = storeEventImageService;
	}

	@Transactional(readOnly = true)
	public MemberNotificationView load(String email) {
		var member = userAccountRepository.findByEmailIgnoreCase(email)
			.filter(account -> account.getRole() == UserRole.MEMBER && account.getStatus() == UserStatus.ACTIVE)
			.flatMap(account -> memberProfileRepository.findByUserAccountId(account.getId()))
			.orElseThrow(() -> new DomainRuleViolationException("Member profile not found."));
		var updates = (member.isCastUpdateNotifications() ? castContentRepository.findFeedForMember(
			member.getId(), ContentStatus.PUBLISHED, UserStatus.ACTIVE, PageRequest.of(0, 20)
		) : java.util.List.<com.castfan.entity.CastContent>of()).stream().map(content -> {
			var access = contentAccessService.access(content, email);
			var cast = content.getCastProfile();
			return new MemberNotificationView.ContentNotice(
				content.getId(), cast.getId(), cast.getStageName(), initials(cast.getStageName()), cast.getAvatarUrl(),
				content.getTitle(), access.accessible() ? preview(content.getBody()) : null,
				contentAccessService.audienceLabel(content), DATE.format(content.getPublishedAt().atZone(DEFAULT_ZONE)),
				access.accessible()
			);
		}).toList();
		var events = (member.isEventNotifications() ? storeEventRepository.findUpcomingPublicEvents(
			EventStatus.PUBLISHED, Instant.now(), PageRequest.of(0, 8)
		) : java.util.List.<com.castfan.entity.StoreEvent>of()).stream().map(event -> {
			var zone = safeZone(event.getStore().getTimezone());
			return new MemberNotificationView.EventNotice(
				event.getId(), event.getTitle(), event.getStore().getName(),
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
