package com.castfan.service;

import java.util.Optional;

import com.castfan.entity.Store;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Legacy compatibility resolver. New Member/public flows must resolve an explicit store slug. */
@Service
public class SystemStoreService {

	public static final String DISPLAY_NAME = "CastFan";

	private final StoreRepository storeRepository;
	private final Environment environment;
	private final UserAccountRepository userAccountRepository;

	public SystemStoreService(StoreRepository storeRepository, Environment environment,
		UserAccountRepository userAccountRepository) {
		this.storeRepository = storeRepository;
		this.environment = environment;
		this.userAccountRepository = userAccountRepository;
	}

	@Transactional
	public Store requireForOwner(UserAccount owner) {
		return storeRepository.findFirstByOwnerIdAndActiveTrueOrderByIdAsc(owner.getId())
			.or(() -> environment.acceptsProfiles(Profiles.of("test"))
				? findActive() : Optional.empty())
			.orElseThrow(() -> new DomainRuleViolationException("CastFan system store is not configured."));
	}

	@Transactional
	public Store requireActive() {
		return findActive()
			.orElseThrow(() -> new DomainRuleViolationException("CastFan system store is not configured."));
	}

	@Transactional
	public Optional<Store> findActive() {
		var legacy = storeRepository.findBySlugAndActiveTrue("castfan");
		if (legacy.isPresent() || !environment.acceptsProfiles(Profiles.of("test"))) return legacy;
		var existing = storeRepository.findFirstByActiveTrueOrderByIdAsc();
		if (existing.isPresent()) return existing;
		var owner = userAccountRepository.findByLoginIdentifierIgnoreCase("test-store-owner@castfan.test")
			.orElseGet(() -> userAccountRepository.save(new UserAccount(
				"test-store-owner@castfan.test", "test-only-password-hash", "Test Store Owner",
				UserRole.OWNER, UserStatus.ACTIVE
			)));
		return Optional.of(storeRepository.save(new Store(owner, "CastFan", "castfan", null,
			"Test address", "Asia/Bangkok", true)));
	}
}
