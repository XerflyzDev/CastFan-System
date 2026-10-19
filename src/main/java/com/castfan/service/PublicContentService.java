package com.castfan.service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import com.castfan.dto.PublicCastProfileView;
import com.castfan.dto.PublicContentDetailView;
import com.castfan.dto.PublicEventView;
import com.castfan.entity.ContentStatus;
import com.castfan.entity.EventStatus;
import com.castfan.entity.ShiftStatus;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastFollowRepository;
import com.castfan.repository.CastContentRepository;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.MemberTierRepository;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PublicContentService {

	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Bangkok");
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH);
	private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);
	private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
	private static final DateTimeFormatter ICS_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

	private final CastProfileRepository castProfileRepository;
	private final StoreCastRepository storeCastRepository;
	private final ShiftScheduleRepository shiftScheduleRepository;
	private final MemberTierRepository memberTierRepository;
	private final CastFollowRepository castFollowRepository;
	private final UserAccountRepository userAccountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final StoreEventRepository storeEventRepository;
	private final CastContentRepository castContentRepository;
	private final ContentAccessService contentAccessService;
	private final StoreEventImageService storeEventImageService;
	private final CastContentImageService castContentImageService;

	public PublicContentService(
		CastProfileRepository castProfileRepository,
		StoreCastRepository storeCastRepository,
		ShiftScheduleRepository shiftScheduleRepository,
		MemberTierRepository memberTierRepository,
		CastFollowRepository castFollowRepository,
		UserAccountRepository userAccountRepository,
		MemberProfileRepository memberProfileRepository,
		StoreEventRepository storeEventRepository,
		CastContentRepository castContentRepository,
		ContentAccessService contentAccessService,
		StoreEventImageService storeEventImageService,
		CastContentImageService castContentImageService
	) {
		this.castProfileRepository = castProfileRepository;
		this.storeCastRepository = storeCastRepository;
		this.shiftScheduleRepository = shiftScheduleRepository;
		this.memberTierRepository = memberTierRepository;
		this.castFollowRepository = castFollowRepository;
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.storeEventRepository = storeEventRepository;
		this.castContentRepository = castContentRepository;
		this.contentAccessService = contentAccessService;
		this.storeEventImageService = storeEventImageService;
		this.castContentImageService = castContentImageService;
	}

	@Transactional(readOnly = true)
	public PublicCastProfileView castProfile(Long castProfileId, String authenticatedEmail) {
		var profile = castProfileRepository.findById(castProfileId)
			.filter(value -> value.isDiscoverable() && value.getUserAccount().getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> notFound("Cast profile not found"));
		return castProfileView(profile, authenticatedEmail);
	}

	@Transactional(readOnly = true)
	public PublicCastProfileView ownCastProfile(String authenticatedEmail) {
		var profile = castProfileRepository.findByUserAccountEmailIgnoreCase(authenticatedEmail)
			.filter(value -> value.getUserAccount().getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> notFound("Cast profile not found"));
		return castProfileView(profile, authenticatedEmail);
	}

	private PublicCastProfileView castProfileView(
		com.castfan.entity.CastProfile profile,
		String authenticatedEmail
	) {
		var castProfileId = profile.getId();
		var assignment = storeCastRepository.findAllByCastProfileIdAndActiveTrue(castProfileId).stream()
			.filter(value -> value.getStore().isActive())
			.findFirst();
		var zone = assignment.map(value -> safeZone(value.getStore().getTimezone())).orElse(DEFAULT_ZONE);
		var now = Instant.now();
		var nextShift = shiftScheduleRepository.findUpcomingPublicShiftsForCast(
			castProfileId, ShiftStatus.APPROVED, UserStatus.ACTIVE, now, now.plusSeconds(90L * 24 * 60 * 60),
			PageRequest.of(0, 1)
		).stream().findFirst().map(shift -> new PublicCastProfileView.ShiftSummary(
			SHORT_DATE.format(shift.getStartsAt().atZone(zone)),
			formatRange(shift.getStartsAt(), shift.getEndsAt(), zone),
			!now.isBefore(shift.getStartsAt()) && now.isBefore(shift.getEndsAt())
		)).orElse(null);

		var tiers = memberTierRepository.findAllByCastProfileIdOrderByRankAsc(castProfileId).stream()
			.filter(tier -> tier.isActive())
			.map(tier -> new PublicCastProfileView.TierSummary(
				tier.getId(), tier.getName(), tier.getDescription(), tier.getMonthlyPrice(), tier.getCurrency()
			)).toList();
		var viewer = memberProfile(authenticatedEmail);
		var isFollowed = viewer
			.map(member -> castFollowRepository.existsByMemberProfileIdAndCastProfileId(member.getId(), castProfileId))
			.orElse(false);
		var content = castContentRepository.findPublishedForProfile(castProfileId, ContentStatus.PUBLISHED).stream()
			.map(post -> {
				var access = contentAccessService.access(post, authenticatedEmail);
				return new PublicCastProfileView.ContentSummary(
					post.getId(), post.getTitle(), access.accessible() ? post.getBody() : null,
					access.accessible() ? castContentImageService.publicUrl(post) : null, contentAccessService.audienceLabel(post),
					SHORT_DATE.format(post.getPublishedAt().atZone(DEFAULT_ZONE)), access.accessible()
				);
			}).toList();

		return new PublicCastProfileView(
			profile.getId(), profile.getStageName(), "@" + profile.getStageNameKey(), profile.getBio(),
			profile.getAvatarUrl(), initials(profile.getStageName()),
			assignment.map(value -> value.getStore().getName()).orElse("CastFan"),
			castFollowRepository.countByCastProfileId(castProfileId), isFollowed,
			nextShift, tiers, content
		);
	}

	@Transactional(readOnly = true)
	public PublicContentDetailView content(Long castProfileId, Long contentId, String authenticatedEmail) {
		var content = castContentRepository.findPublishedByIdForProfile(
			contentId, castProfileId, ContentStatus.PUBLISHED, UserStatus.ACTIVE
		).orElseThrow(() -> notFound("Content not found"));
		var access = contentAccessService.access(content, authenticatedEmail);
		var profile = content.getCastProfile();
		return new PublicContentDetailView(
			content.getId(), profile.getId(), profile.getStageName(), initials(profile.getStageName()),
			profile.getAvatarUrl(), content.getTitle(), access.accessible() ? content.getBody() : null,
			access.accessible() ? castContentImageService.publicUrl(content) : null, contentAccessService.audienceLabel(content),
			DATE.format(content.getPublishedAt().atZone(DEFAULT_ZONE)), access.accessible()
		);
	}

	@Transactional(readOnly = true)
	public PublicEventView event(Long eventId) {
		var event = storeEventRepository.findPublicById(eventId, EventStatus.PUBLISHED)
			.orElseThrow(() -> notFound("Event not found"));
		var zone = safeZone(event.getStore().getTimezone());
		var castByProfile = shiftScheduleRepository.findPublicShiftsInRange(
			event.getStore().getId(), ShiftStatus.APPROVED, UserStatus.ACTIVE,
			event.getStartsAt(), event.getEndsAt()
		).stream().collect(Collectors.toMap(shift -> shift.getStoreCast().getCastProfile().getId(), shift -> {
			var profile = shift.getStoreCast().getCastProfile();
			return new PublicEventView.CastSummary(
				profile.getId(), profile.getStageName(), initials(profile.getStageName()), profile.getAvatarUrl(),
				formatRange(max(shift.getStartsAt(), event.getStartsAt()), min(shift.getEndsAt(), event.getEndsAt()), zone)
			);
		}, (first, ignored) -> first, java.util.LinkedHashMap::new));

		return new PublicEventView(
			event.getId(), event.getTitle(), event.getDescription(), storeEventImageService.publicImageUrl(event),
			DATE.format(event.getStartsAt().atZone(zone)), formatRange(event.getStartsAt(), event.getEndsAt(), zone),
			event.getStore().getName(),
			event.getStore().getAddress() == null ? event.getStore().getName() : event.getStore().getAddress(),
			java.util.List.copyOf(castByProfile.values())
		);
	}

	@Transactional(readOnly = true)
	public String eventCalendar(Long eventId) {
		var event = storeEventRepository.findPublicById(eventId, EventStatus.PUBLISHED)
			.orElseThrow(() -> notFound("Event not found"));
		return "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//CastFan//Events//EN\r\nBEGIN:VEVENT\r\n"
			+ "UID:event-" + event.getId() + "@castfan\r\n"
			+ "DTSTAMP:" + ICS_DATE_TIME.format(Instant.now().atZone(ZoneId.of("UTC"))) + "\r\n"
			+ "DTSTART:" + ICS_DATE_TIME.format(event.getStartsAt().atZone(ZoneId.of("UTC"))) + "\r\n"
			+ "DTEND:" + ICS_DATE_TIME.format(event.getEndsAt().atZone(ZoneId.of("UTC"))) + "\r\n"
			+ "SUMMARY:" + ics(event.getTitle()) + "\r\n"
			+ "DESCRIPTION:" + ics(event.getDescription()) + "\r\n"
			+ "LOCATION:" + ics(event.getStore().getAddress() == null ? event.getStore().getName() : event.getStore().getAddress()) + "\r\n"
			+ "END:VEVENT\r\nEND:VCALENDAR\r\n";
	}

	private boolean followed(String email, Long castProfileId) {
		return memberProfile(email)
			.map(member -> castFollowRepository.existsByMemberProfileIdAndCastProfileId(member.getId(), castProfileId))
			.orElse(false);
	}

	private java.util.Optional<com.castfan.entity.MemberProfile> memberProfile(String email) {
		if (email == null) return java.util.Optional.empty();
		return userAccountRepository.findByEmailIgnoreCase(email)
			.filter(account -> (account.getRole() == UserRole.MEMBER || account.getRole() == UserRole.CAST)
				&& account.getStatus() == UserStatus.ACTIVE)
			.flatMap(account -> memberProfileRepository.findByUserAccountId(account.getId()));
	}

	private ZoneId safeZone(String timezone) {
		try { return ZoneId.of(timezone); } catch (DateTimeException exception) { return DEFAULT_ZONE; }
	}

	private String formatRange(Instant startsAt, Instant endsAt, ZoneId zone) {
		return TIME.format(startsAt.atZone(zone)) + " - " + TIME.format(endsAt.atZone(zone));
	}

	private String initials(String name) {
		return Arrays.stream(name.trim().split("\\s+")).filter(part -> !part.isEmpty()).limit(2)
			.map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH)).collect(Collectors.joining());
	}

	private String ics(String value) {
		return value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
			.replace("\r\n", "\\n").replace("\n", "\\n");
	}

	private Instant max(Instant first, Instant second) { return first.isAfter(second) ? first : second; }
	private Instant min(Instant first, Instant second) { return first.isBefore(second) ? first : second; }
	private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
}
