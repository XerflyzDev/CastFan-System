package com.castfan.service;

import com.castfan.dto.StoreSettingsForm;
import com.castfan.entity.Store;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerSettingsService {
	private final UserAccountRepository accounts;
	private final StoreRepository stores;

	public OwnerSettingsService(UserAccountRepository accounts, StoreRepository stores) {
		this.accounts = accounts; this.stores = stores;
	}

	@Transactional(readOnly = true)
	public Store requireStore(String ownerIdentifier) {
		var owner = accounts.findByLoginIdentifierIgnoreCase(ownerIdentifier)
			.filter(a -> a.getRole() == UserRole.OWNER && a.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Owner account not found."));
		return stores.findAllByOwnerIdOrderByNameAsc(owner.getId()).stream().findFirst()
			.orElseThrow(() -> new DomainRuleViolationException("Store not found."));
	}

	@Transactional(readOnly = true)
	public StoreSettingsForm form(String ownerIdentifier) {
		var store = requireStore(ownerIdentifier);
		var form = new StoreSettingsForm(); form.setName(store.getName());
		form.setDescription(store.getDescription()); form.setAddress(store.getAddress()); return form;
	}

	@Transactional
	public Store update(String ownerIdentifier, StoreSettingsForm form) {
		var store = requireStore(ownerIdentifier);
		store.updateDetails(form.getName().trim(), clean(form.getDescription()), form.getAddress().trim(), store.getTimezone());
		return stores.saveAndFlush(store);
	}

	private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
