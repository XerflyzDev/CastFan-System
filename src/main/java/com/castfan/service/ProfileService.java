package com.castfan.service;

import java.text.Normalizer;
import java.util.Locale;

import com.castfan.dto.MemberProfileForm;
import com.castfan.dto.MemberProfileView;
import com.castfan.entity.CastProfile;
import com.castfan.entity.MemberProfile;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileService {

	private final MemberProfileRepository memberProfileRepository;
	private final CastProfileRepository castProfileRepository;
	private final AccountAvatarService accountAvatarService;

	public ProfileService(
		MemberProfileRepository memberProfileRepository,
		CastProfileRepository castProfileRepository,
		AccountAvatarService accountAvatarService
	) {
		this.memberProfileRepository = memberProfileRepository;
		this.castProfileRepository = castProfileRepository;
		this.accountAvatarService = accountAvatarService;
	}

	@Transactional
	public MemberProfile createMemberProfile(UserAccount account) {
		requireAudienceAccount(account);
		if (memberProfileRepository.existsByUserAccountId(account.getId())) {
			throw new DomainConflictException("This account already has a Member profile.");
		}

		try {
			return memberProfileRepository.saveAndFlush(new MemberProfile(account, null, null));
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("This account already has a Member profile.");
		}
	}

	@Transactional(readOnly = true)
	public MemberProfileView memberProfile(String email) {
		return MemberProfileView.from(requireMemberProfile(email));
	}

	@Transactional(readOnly = true)
	public MemberProfileForm memberProfileForm(String email) {
		var profile = requireMemberProfile(email);
		var form = new MemberProfileForm();
		form.setDisplayName(profile.getUserAccount().getDisplayName());
		form.setBio(profile.getBio());
		form.setCastUpdateNotifications(profile.isCastUpdateNotifications());
		form.setEventNotifications(profile.isEventNotifications());
		return form;
	}

	@Transactional
	public MemberProfileView updateMemberProfile(String email, MemberProfileForm form, MultipartFile image) {
		var profile = requireMemberProfile(email);
		var displayName = requireText(form.getDisplayName(), "Display name", 80);
		var bio = cleanOptionalText(form.getBio(), "Bio", 500);
		profile.getUserAccount().updateDisplayName(displayName);
		profile.updateDetails(bio, profile.getAvatarUrl(), form.isCastUpdateNotifications(), form.isEventNotifications());
		var saved = memberProfileRepository.saveAndFlush(profile);
		updateAvatar(email, image, form.isRemoveImage());
		return MemberProfileView.from(saved);
	}

	@Transactional
	public void updateMemberNotificationPreference(String email, String preference, boolean enabled) {
		var profile = requireMemberProfile(email);
		switch (preference) {
			case "castUpdateNotifications" -> profile.updateDetails(
				profile.getBio(), profile.getAvatarUrl(), enabled, profile.isEventNotifications()
			);
			case "eventNotifications" -> profile.updateDetails(
				profile.getBio(), profile.getAvatarUrl(), profile.isCastUpdateNotifications(), enabled
			);
			default -> throw new DomainRuleViolationException("Invalid notification preference.");
		}
		memberProfileRepository.saveAndFlush(profile);
	}

	@Transactional
	public CastProfile createCastProfile(
		UserAccount account,
		String stageName,
		String bio,
		String avatarUrl,
		boolean discoverable
	) {
		requireEligibleAccount(account, UserRole.CAST);
		if (castProfileRepository.existsByUserAccountId(account.getId())) {
			throw new DomainConflictException("This account already has a Cast profile.");
		}

		var cleanStageName = requireText(stageName, "Stage name", 80);
		var stageNameKey = normalizeKey(cleanStageName);
		if (castProfileRepository.existsByStageNameKey(stageNameKey)) {
			throw new DomainConflictException("This stage name is already in use.");
		}

		var profile = new CastProfile(
			account,
			cleanStageName,
			stageNameKey,
			cleanOptionalText(bio, "Bio", 1000),
			cleanOptionalText(avatarUrl, "Avatar URL", 2048),
			discoverable
		);

		try {
			return castProfileRepository.saveAndFlush(profile);
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("The account or stage name is already in use.");
		}
	}

	@Transactional
	public CastProfile updateCastProfile(
		String email,
		String stageName,
		String bio,
		boolean discoverable,
		MultipartFile image,
		boolean removeImage
	) {
		var profile = castProfileRepository.findByUserAccountEmailIgnoreCase(email)
			.orElseThrow(() -> new DomainRuleViolationException("Cast profile not found."));
		var cleanStageName = requireText(stageName, "Stage name", 80);
		var stageNameKey = normalizeKey(cleanStageName);
		castProfileRepository.findByStageNameKey(stageNameKey)
			.filter(existing -> !existing.getId().equals(profile.getId()))
			.ifPresent(existing -> {
				throw new DomainConflictException("This stage name is already in use.");
			});

		profile.updatePublicDetails(
			cleanStageName,
			stageNameKey,
			cleanOptionalText(bio, "Bio", 1000),
			profile.getAvatarUrl(),
			discoverable
		);
		try {
			var saved = castProfileRepository.saveAndFlush(profile);
			updateAvatar(email, image, removeImage);
			return saved;
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("This stage name is already in use.");
		}
	}

	private void updateAvatar(String email, MultipartFile image, boolean removeImage) {
		if (image != null && !image.isEmpty()) accountAvatarService.upload(email, image);
		else if (removeImage) accountAvatarService.remove(email);
	}

	private void requireEligibleAccount(UserAccount account, UserRole requiredRole) {
		if (account == null || account.getId() == null) {
			throw new DomainRuleViolationException("A saved account is required.");
		}
		if (account.getStatus() != UserStatus.ACTIVE) {
			throw new DomainRuleViolationException("The account must be active.");
		}
		if (account.getRole() != requiredRole) {
			throw new DomainRuleViolationException("The account does not have the required role.");
		}
	}

	private void requireAudienceAccount(UserAccount account) {
		if (account == null || account.getId() == null) {
			throw new DomainRuleViolationException("A saved account is required.");
		}
		if (account.getStatus() != UserStatus.ACTIVE) {
			throw new DomainRuleViolationException("The account must be active.");
		}
		if (account.getRole() != UserRole.MEMBER && account.getRole() != UserRole.CAST) {
			throw new DomainRuleViolationException("A Member or Cast account is required.");
		}
	}

	private MemberProfile requireMemberProfile(String email) {
		return memberProfileRepository.findByUserAccountEmailIgnoreCase(email)
			.filter(profile -> (profile.getUserAccount().getRole() == UserRole.MEMBER
					|| profile.getUserAccount().getRole() == UserRole.CAST)
				&& profile.getUserAccount().getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Member profile not found."));
	}

	private String normalizeKey(String value) {
		return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
	}

	private String requireText(String value, String fieldName, int maximumLength) {
		var cleaned = value == null ? "" : value.trim();
		if (cleaned.isEmpty()) {
			throw new DomainRuleViolationException(fieldName + " is required.");
		}
		if (cleaned.length() > maximumLength) {
			throw new DomainRuleViolationException(fieldName + " is too long.");
		}
		return cleaned;
	}

	private String cleanOptionalText(String value, String fieldName, int maximumLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return requireText(value, fieldName, maximumLength);
	}
}
