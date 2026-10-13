package com.castfan.service;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Pattern;

import com.castfan.entity.Store;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.StoreRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreService {

	private static final Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

	private final StoreRepository storeRepository;

	public StoreService(StoreRepository storeRepository) {
		this.storeRepository = storeRepository;
	}

	@Transactional
	public Store createStore(
		UserAccount owner,
		String name,
		String slug,
		String description,
		String address,
		String timezone
	) {
		requireEligibleOwner(owner);
		var cleanName = requireText(name, "Store name", 120);
		var cleanSlug = normalizeSlug(slug);
		var cleanTimezone = validateTimezone(timezone);

		if (storeRepository.existsBySlug(cleanSlug)) {
			throw new DomainConflictException("This store slug is already in use.");
		}

		var store = new Store(
			owner,
			cleanName,
			cleanSlug,
			cleanOptionalText(description, "Description", 1000),
			cleanOptionalText(address, "Address", 500),
			cleanTimezone,
			true
		);

		try {
			return storeRepository.saveAndFlush(store);
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("This store slug is already in use.");
		}
	}

	private void requireEligibleOwner(UserAccount owner) {
		if (owner == null || owner.getId() == null) {
			throw new DomainRuleViolationException("A saved Owner account is required.");
		}
		if (owner.getStatus() != UserStatus.ACTIVE || owner.getRole() != UserRole.OWNER) {
			throw new DomainRuleViolationException("An active Owner account is required.");
		}
	}

	private String normalizeSlug(String slug) {
		var cleaned = requireText(slug, "Store slug", 100).toLowerCase(Locale.ROOT);
		if (!SLUG_PATTERN.matcher(cleaned).matches()) {
			throw new DomainRuleViolationException("Store slug must contain lowercase letters, numbers, and single hyphens only.");
		}
		return cleaned;
	}

	private String validateTimezone(String timezone) {
		var cleaned = requireText(timezone, "Timezone", 64);
		try {
			return ZoneId.of(cleaned).getId();
		} catch (DateTimeException exception) {
			throw new DomainRuleViolationException("Timezone must be a valid IANA time zone.");
		}
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
