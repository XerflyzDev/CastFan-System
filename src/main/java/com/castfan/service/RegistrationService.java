package com.castfan.service;

import java.util.Locale;

import com.castfan.dto.RegistrationForm;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

	private final UserAccountRepository userAccountRepository;
	private final PasswordEncoder passwordEncoder;
	private final ProfileService profileService;

	public RegistrationService(
		UserAccountRepository userAccountRepository,
		PasswordEncoder passwordEncoder,
		ProfileService profileService
	) {
		this.userAccountRepository = userAccountRepository;
		this.passwordEncoder = passwordEncoder;
		this.profileService = profileService;
	}

	@Transactional
	public UserAccount registerMember(RegistrationForm form) {
		var email = normalizeEmail(form.getEmail());
		if (userAccountRepository.existsByEmailIgnoreCase(email)) {
			throw new DuplicateEmailException();
		}

		var account = new UserAccount(
			email,
			passwordEncoder.encode(form.getPassword()),
			form.getDisplayName().trim(),
			UserRole.MEMBER,
			UserStatus.ACTIVE
		);

		try {
			var savedAccount = userAccountRepository.saveAndFlush(account);
			profileService.createMemberProfile(savedAccount);
			return savedAccount;
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateEmailException();
		}
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
