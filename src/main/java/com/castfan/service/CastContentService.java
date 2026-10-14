package com.castfan.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.castfan.dto.CastContentForm;
import com.castfan.dto.CastContentView;
import com.castfan.entity.CastContent;
import com.castfan.entity.CastProfile;
import com.castfan.entity.ContentAudience;
import com.castfan.entity.ContentStatus;
import com.castfan.entity.MemberTier;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastContentRepository;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberTierRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CastContentService {
	private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Bangkok");
	private static final DateTimeFormatter PUBLISHED = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.ENGLISH);

	private final UserAccountRepository userAccountRepository;
	private final CastProfileRepository castProfileRepository;
	private final MemberTierRepository memberTierRepository;
	private final CastContentRepository castContentRepository;

	public CastContentService(UserAccountRepository userAccountRepository, CastProfileRepository castProfileRepository,
		MemberTierRepository memberTierRepository, CastContentRepository castContentRepository) {
		this.userAccountRepository = userAccountRepository;
		this.castProfileRepository = castProfileRepository;
		this.memberTierRepository = memberTierRepository;
		this.castContentRepository = castContentRepository;
	}

	@Transactional(readOnly = true)
	public CastContentView load(String email, Long editingContentId) {
		var profile = requireCast(email);
		return new CastContentView(
			castContentRepository.findAllOwned(email).stream().map(this::toItem).toList(),
			memberTierRepository.findAllByCastProfileIdOrderByRankAsc(profile.getId()).stream()
				.map(tier -> new CastContentView.TierOption(tier.getId(), tier.getName(), tier.getRank(), tier.isActive()))
				.toList(),
			editingContentId
		);
	}

	@Transactional(readOnly = true)
	public CastContentForm formForEdit(String email, Long contentId) {
		var content = requireOwned(email, contentId, false);
		if (content.getStatus() == ContentStatus.ARCHIVED) {
			throw new DomainRuleViolationException("Archived content cannot be edited.");
		}
		var form = new CastContentForm();
		form.setTitle(content.getTitle());
		form.setBody(content.getBody());
		form.setImageUrl(content.getImageUrl());
		form.setAudience(content.getAudience());
		form.setMinimumTierId(content.getMinimumTier() == null ? null : content.getMinimumTier().getId());
		return form;
	}

	@Transactional
	public void create(String email, CastContentForm form) {
		var profile = requireCast(email);
		var tier = resolveTier(profile, form);
		castContentRepository.saveAndFlush(new CastContent(
			profile, cleanRequired(form.getTitle(), "Title", 160), cleanRequired(form.getBody(), "Content", 5000),
			cleanOptional(form.getImageUrl()), form.getAudience(), tier
		));
	}

	@Transactional
	public void update(String email, Long contentId, CastContentForm form) {
		var profile = requireCast(email);
		var content = requireOwnedForUpdate(profile, contentId);
		try {
			content.updateDetails(
				cleanRequired(form.getTitle(), "Title", 160), cleanRequired(form.getBody(), "Content", 5000),
				cleanOptional(form.getImageUrl()), form.getAudience(), resolveTier(profile, form)
			);
		} catch (IllegalArgumentException | IllegalStateException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
		castContentRepository.saveAndFlush(content);
	}

	@Transactional
	public void publish(String email, Long contentId) {
		var content = requireOwnedForUpdate(requireCast(email), contentId);
		try { content.publish(); } catch (IllegalStateException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
		castContentRepository.saveAndFlush(content);
	}

	@Transactional
	public void archive(String email, Long contentId) {
		var content = requireOwnedForUpdate(requireCast(email), contentId);
		try { content.archive(); } catch (IllegalStateException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
		castContentRepository.saveAndFlush(content);
	}

	private MemberTier resolveTier(CastProfile profile, CastContentForm form) {
		if (form.getAudience() == null) throw new DomainRuleViolationException("Audience is required.");
		if (form.getAudience() != ContentAudience.MEMBERS) {
			if (form.getMinimumTierId() != null) throw new DomainRuleViolationException("Only member content can require a tier.");
			return null;
		}
		if (form.getMinimumTierId() == null) throw new DomainRuleViolationException("Choose a minimum tier for member content.");
		return memberTierRepository.findById(form.getMinimumTierId())
			.filter(tier -> tier.getCastProfile().getId().equals(profile.getId()) && tier.isActive())
			.orElseThrow(() -> new DomainRuleViolationException("Active membership tier not found."));
	}

	private CastProfile requireCast(String email) {
		var account = userAccountRepository.findByEmailIgnoreCase(email)
			.filter(value -> value.getRole() == UserRole.CAST && value.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Active Cast account not found."));
		return castProfileRepository.findByUserAccountId(account.getId())
			.orElseThrow(() -> new DomainRuleViolationException("Cast profile not found."));
	}

	private CastContent requireOwned(String email, Long contentId, boolean forUpdate) {
		var profile = requireCast(email);
		var value = forUpdate ? castContentRepository.findByIdForUpdate(contentId) : castContentRepository.findById(contentId);
		return value.filter(content -> content.getCastProfile().getId().equals(profile.getId()))
			.orElseThrow(() -> new DomainRuleViolationException("Content not found."));
	}

	private CastContent requireOwnedForUpdate(CastProfile profile, Long contentId) {
		return castContentRepository.findByIdForUpdate(contentId)
			.filter(content -> content.getCastProfile().getId().equals(profile.getId()))
			.orElseThrow(() -> new DomainRuleViolationException("Content not found."));
	}

	private CastContentView.ContentItem toItem(CastContent content) {
		return new CastContentView.ContentItem(
			content.getId(), content.getTitle(), content.getBody(), content.getImageUrl(), audienceLabel(content),
			content.getMinimumTier() == null ? null : content.getMinimumTier().getName(),
			statusLabel(content.getStatus()), content.getStatus().name().toLowerCase(Locale.ENGLISH),
			content.getPublishedAt() == null ? null : PUBLISHED.format(content.getPublishedAt().atZone(DISPLAY_ZONE)),
			content.getStatus() != ContentStatus.ARCHIVED, content.getStatus() == ContentStatus.DRAFT,
			content.getStatus() != ContentStatus.ARCHIVED
		);
	}

	private String audienceLabel(CastContent content) {
		return switch (content.getAudience()) {
			case PUBLIC -> "Public";
			case FOLLOWERS -> "Followers";
			case MEMBERS -> content.getMinimumTier().getName() + "+";
		};
	}

	private String statusLabel(ContentStatus status) {
		return switch (status) { case DRAFT -> "Draft"; case PUBLISHED -> "Published"; case ARCHIVED -> "Archived"; };
	}

	private String cleanRequired(String value, String field, int maximum) {
		var clean = value == null ? "" : value.trim();
		if (clean.isEmpty()) throw new DomainRuleViolationException(field + " is required.");
		if (clean.length() > maximum) throw new DomainRuleViolationException(field + " is too long.");
		return clean;
	}

	private String cleanOptional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
