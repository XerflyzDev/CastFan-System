package com.castfan.service;

import java.text.Normalizer;
import java.util.Locale;

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

@Service
public class ProfileService {

	private final MemberProfileRepository memberProfileRepository;
	private final CastProfileRepository castProfileRepository;

	public ProfileService(
		MemberProfileRepository memberProfileRepository,
		CastProfileRepository castProfileRepository
	) {
		this.memberProfileRepository = memberProfileRepository;
		this.castProfileRepository = castProfileRepository;
	}

	@Transactional
	public MemberProfile createMemberProfile(UserAccount account) {
		requireEligibleAccount(account, UserRole.MEMBER);
		if (memberProfileRepository.existsByUserAccountId(account.getId())) {
			throw new DomainConflictException("This account already has a Member profile.");
		}

		try {
			return memberProfileRepository.saveAndFlush(new MemberProfile(account, null, null));
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("This account already has a Member profile.");
		}
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
		String avatarUrl,
		boolean discoverable
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
			cleanOptionalText(avatarUrl, "Avatar URL", 2048),
			discoverable
		);
		try {
			return castProfileRepository.saveAndFlush(profile);
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("This stage name is already in use.");
		}
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
