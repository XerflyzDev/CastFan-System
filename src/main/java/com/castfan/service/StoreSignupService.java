package com.castfan.service;

import java.text.Normalizer;
import java.util.Locale;

import com.castfan.dto.StoreSignupForm;
import com.castfan.entity.Store;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreSignupService {
	private final UserAccountRepository accounts;
	private final StoreRepository stores;
	private final PasswordEncoder passwordEncoder;

	public StoreSignupService(UserAccountRepository accounts, StoreRepository stores, PasswordEncoder passwordEncoder) {
		this.accounts = accounts;
		this.stores = stores;
		this.passwordEncoder = passwordEncoder;
	}

	public String suggestSlug(String name) {
		var normalized = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
			.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
			.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		return normalized.isBlank() ? "my-store" : normalized.substring(0, Math.min(100, normalized.length()));
	}

	@Transactional
	public Store register(StoreSignupForm form) {
		var email = form.getEmail().trim().toLowerCase(Locale.ROOT);
		var slug = form.getSlug().trim().toLowerCase(Locale.ROOT);
		if (accounts.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("This email is already registered.");
		if (stores.existsBySlug(slug)) throw new IllegalArgumentException("This store URL is already in use.");
		var owner = accounts.save(new UserAccount(email, email, passwordEncoder.encode(form.getPassword()),
			form.getOwnerName().trim(), UserRole.OWNER, UserStatus.ACTIVE, false));
		try {
			return stores.saveAndFlush(new Store(owner, form.getStoreName().trim(), slug,
				clean(form.getDescription()), form.getAddress().trim(), "Asia/Bangkok", true));
		} catch (DataIntegrityViolationException exception) {
			throw new IllegalArgumentException("The email, owner, or store URL is already in use.", exception);
		}
	}

	private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
