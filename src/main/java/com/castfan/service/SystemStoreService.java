package com.castfan.service;

import java.util.Optional;

import com.castfan.entity.Store;
import com.castfan.entity.UserAccount;
import com.castfan.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves the single cafe configured for the CastFan application. */
@Service
public class SystemStoreService {

	public static final String DISPLAY_NAME = "CastFan";

	private final StoreRepository storeRepository;

	public SystemStoreService(StoreRepository storeRepository) {
		this.storeRepository = storeRepository;
	}

	@Transactional(readOnly = true)
	public Store requireForOwner(UserAccount owner) {
		return storeRepository.findFirstByOwnerIdAndActiveTrueOrderByIdAsc(owner.getId())
			.orElseThrow(() -> new DomainRuleViolationException("CastFan system store is not configured."));
	}

	@Transactional(readOnly = true)
	public Store requireActive() {
		return findActive()
			.orElseThrow(() -> new DomainRuleViolationException("CastFan system store is not configured."));
	}

	@Transactional(readOnly = true)
	public Optional<Store> findActive() {
		return storeRepository.findFirstByActiveTrueOrderByIdAsc();
	}
}
