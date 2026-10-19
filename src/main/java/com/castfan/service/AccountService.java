package com.castfan.service;

import com.castfan.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

	private final UserAccountRepository userAccountRepository;

	public AccountService(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional
	public void updateDisplayName(String email, String displayName) {
		var cleanedName = displayName == null ? "" : displayName.trim();
		if (cleanedName.length() < 2) {
			throw new DomainRuleViolationException("Name must contain at least 2 characters.");
		}
		if (cleanedName.length() > 80) {
			throw new DomainRuleViolationException("Name must not exceed 80 characters.");
		}

		var account = userAccountRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new UsernameNotFoundException("Account not found"));
		account.updateDisplayName(cleanedName);
		userAccountRepository.save(account);
	}
}
