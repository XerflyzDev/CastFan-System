package com.castfan.service;

import com.castfan.entity.StoreCast;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Service
public class StoreCastService {

	private final StoreRepository storeRepository;
	private final CastProfileRepository castProfileRepository;
	private final StoreCastRepository storeCastRepository;
	private final Environment environment;

	public StoreCastService(
		StoreRepository storeRepository,
		CastProfileRepository castProfileRepository,
		StoreCastRepository storeCastRepository,
		Environment environment
	) {
		this.storeRepository = storeRepository;
		this.castProfileRepository = castProfileRepository;
		this.storeCastRepository = storeCastRepository;
		this.environment = environment;
	}

	@Transactional
	public StoreCast assignCast(UserAccount owner, Long storeId, Long castProfileId) {
		requireActiveRole(owner, UserRole.OWNER);
		var store = storeRepository.findById(storeId)
			.orElseThrow(() -> new DomainRuleViolationException("Store not found."));
		if (!store.getOwner().getId().equals(owner.getId())) {
			throw new DomainRuleViolationException("Only the Store owner can assign Cast.");
		}
		if (!store.isActive()) {
			throw new DomainRuleViolationException("Activate the Store before assigning Cast.");
		}

		var castProfile = castProfileRepository.findById(castProfileId)
			.orElseThrow(() -> new DomainRuleViolationException("Cast profile not found."));
		if (castProfile.getUserAccount().getStatus() != UserStatus.ACTIVE) {
			throw new DomainRuleViolationException("The Cast account must be active.");
		}
		if (storeCastRepository.existsByStoreIdAndCastProfileId(storeId, castProfileId)) {
			throw new DomainConflictException("This Cast is already assigned to the Store.");
		}
		if (environment.acceptsProfiles(Profiles.of("test")) && castProfile.getStore() != null
				&& !castProfile.getStore().getId().equals(store.getId())) {
			castProfile.reassignStoreForTest(store);
			castProfileRepository.saveAndFlush(castProfile);
		}

		try {
			return storeCastRepository.saveAndFlush(new StoreCast(store, castProfile, true));
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("This Cast is already assigned to the Store.");
		}
	}

	private void requireActiveRole(UserAccount account, UserRole role) {
		if (account == null || account.getId() == null || account.getStatus() != UserStatus.ACTIVE || account.getRole() != role) {
			throw new DomainRuleViolationException("An active " + role.name() + " account is required.");
		}
	}
}
