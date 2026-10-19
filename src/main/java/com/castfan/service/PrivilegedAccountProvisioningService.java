package com.castfan.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivilegedAccountProvisioningService {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final ProfileService profileService;

	public PrivilegedAccountProvisioningService(
		UserAccountRepository userAccountRepository,
		PasswordEncoder passwordEncoder,
		ProfileService profileService
	) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.profileService = profileService;
	}

	@Transactional
	public UserAccount provisionCast(String email, String password, String displayName, String stageName) {
		var account = createAccount(email, password, displayName, UserRole.CAST);
		profileService.createMemberProfile(account);
		profileService.createCastProfile(account, stageName, null, null, false);
		return account;
	}

	@Transactional
	public UserAccount provisionOwner(String email, String password, String displayName) {
		return createAccount(email, password, displayName, UserRole.OWNER);
	}

	private UserAccount createAccount(String email, String password, String displayName, UserRole role) {
		var cleanEmail = normalizeEmail(email);
		var cleanDisplayName = requireText(displayName, "Display name", 2, 80);
		validatePassword(password);

		if (userAccountRepository.existsByEmailIgnoreCase(cleanEmail)) {
			throw new DuplicateEmailException();
		}

		var account = new UserAccount(
			cleanEmail,
			passwordEncoder.encode(password),
			cleanDisplayName,
			role,
			UserStatus.ACTIVE
		);

		try {
			return userAccountRepository.saveAndFlush(account);
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateEmailException();
		}
	}

	private String normalizeEmail(String email) {
		var cleaned = requireText(email, "Email", 3, 254).toLowerCase(Locale.ROOT);
		if (!EMAIL_PATTERN.matcher(cleaned).matches()) {
			throw new DomainRuleViolationException("Email must be valid.");
		}
		return cleaned;
	}

	private void validatePassword(String password) {
		if (password == null || password.length() < 12) {
			throw new DomainRuleViolationException("Privileged account password must contain at least 12 characters.");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new DomainRuleViolationException("Password must not exceed 72 bytes.");
		}
	}

	private String requireText(String value, String fieldName, int minimumLength, int maximumLength) {
		var cleaned = value == null ? "" : value.trim();
		if (cleaned.length() < minimumLength || cleaned.length() > maximumLength) {
			throw new DomainRuleViolationException(
				fieldName + " must be between " + minimumLength + " and " + maximumLength + " characters."
			);
		}
		return cleaned;
	}
}
