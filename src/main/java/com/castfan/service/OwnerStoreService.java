package com.castfan.service;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import com.castfan.dto.OwnerCastAssignmentForm;
import com.castfan.dto.OwnerStoreForm;
import com.castfan.dto.OwnerStoreView;
import com.castfan.entity.Store;
import com.castfan.entity.StoreCast;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerStoreService {

	private final UserAccountRepository userAccountRepository;
	private final StoreRepository storeRepository;
	private final StoreCastRepository storeCastRepository;
	private final CastProfileRepository castProfileRepository;
	private final StoreService storeService;

	public OwnerStoreService(
		UserAccountRepository userAccountRepository,
		StoreRepository storeRepository,
		StoreCastRepository storeCastRepository,
		CastProfileRepository castProfileRepository,
		StoreService storeService
	) {
		this.userAccountRepository = userAccountRepository;
		this.storeRepository = storeRepository;
		this.storeCastRepository = storeCastRepository;
		this.castProfileRepository = castProfileRepository;
		this.storeService = storeService;
	}

	@Transactional(readOnly = true)
	public OwnerStoreView load(String email, Long editingStoreId) {
		var owner = requireOwner(email);
		var stores = storeRepository.findAllByOwnerIdOrderByNameAsc(owner.getId());
		var assignments = storeCastRepository.findAllForOwner(email);
		return new OwnerStoreView(
			stores.stream().map(store -> new OwnerStoreView.StoreItem(
				store.getId(), store.getName(), store.getSlug(), store.getDescription(), store.getAddress(),
				store.getTimezone(), store.isActive(), assignments.stream()
					.filter(assignment -> assignment.isActive() && assignment.getStore().getId().equals(store.getId()))
					.count()
			)).toList(),
			castProfileRepository.findAllByUserAccountStatusOrderByStageNameAsc(UserStatus.ACTIVE).stream()
				.map(profile -> new OwnerStoreView.CastOption(
					profile.getId(), profile.getStageName(), profile.getUserAccount().getEmail()
				)).toList(),
			assignments.stream().map(this::toAssignmentItem).toList(),
			editingStoreId
		);
	}

	@Transactional(readOnly = true)
	public OwnerStoreForm formForEdit(String email, Long storeId) {
		var store = requireOwnedStore(requireOwner(email), storeId);
		var form = new OwnerStoreForm();
		form.setName(store.getName());
		form.setSlug(store.getSlug());
		form.setDescription(store.getDescription());
		form.setAddress(store.getAddress());
		form.setTimezone(store.getTimezone());
		return form;
	}

	@Transactional
	public void create(String email, OwnerStoreForm form) {
		var owner = requireOwner(email);
		storeService.createStore(
			owner, form.getName(), form.getSlug(), form.getDescription(), form.getAddress(), form.getTimezone()
		);
	}

	@Transactional
	public void update(String email, Long storeId, OwnerStoreForm form) {
		var store = requireOwnedStore(requireOwner(email), storeId);
		store.updateDetails(
			cleanRequired(form.getName(), "Store name", 120),
			cleanOptional(form.getDescription(), "Description", 1000),
			cleanOptional(form.getAddress(), "Address", 500),
			validTimezone(form.getTimezone())
		);
		storeRepository.saveAndFlush(store);
	}

	@Transactional
	public void setStoreActive(String email, Long storeId, boolean active) {
		var store = requireOwnedStore(requireOwner(email), storeId);
		if (active) store.activate(); else store.deactivate();
		storeRepository.saveAndFlush(store);
	}

	@Transactional
	public void assignCast(String email, OwnerCastAssignmentForm form) {
		var owner = requireOwner(email);
		var store = requireOwnedStore(owner, form.getStoreId());
		if (!store.isActive()) {
			throw new DomainRuleViolationException("Activate the store before assigning Cast.");
		}
		var profile = castProfileRepository.findById(form.getCastProfileId())
			.filter(value -> value.getUserAccount().getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Active Cast profile not found."));
		var existing = storeCastRepository.findByStoreIdAndCastProfileId(store.getId(), profile.getId());
		if (existing.isPresent()) {
			var assignment = existing.get();
			if (assignment.isActive()) {
				throw new DomainConflictException("This Cast is already assigned to the store.");
			}
			assignment.activate();
			storeCastRepository.saveAndFlush(assignment);
			return;
		}
		storeCastRepository.saveAndFlush(new StoreCast(store, profile, true));
	}

	@Transactional
	public void setAssignmentActive(String email, Long assignmentId, boolean active) {
		var owner = requireOwner(email);
		var assignment = storeCastRepository.findByIdForUpdate(assignmentId)
			.filter(value -> value.getStore().getOwner().getId().equals(owner.getId()))
			.orElseThrow(() -> new DomainRuleViolationException("Store Cast assignment not found."));
		if (active && !assignment.getStore().isActive()) {
			throw new DomainRuleViolationException("Activate the store before restoring Cast.");
		}
		if (active) assignment.activate(); else assignment.deactivate();
		storeCastRepository.saveAndFlush(assignment);
	}

	private OwnerStoreView.AssignmentItem toAssignmentItem(StoreCast assignment) {
		var profile = assignment.getCastProfile();
		return new OwnerStoreView.AssignmentItem(
			assignment.getId(), assignment.getStore().getId(), assignment.getStore().getName(), profile.getId(),
			profile.getStageName(), profile.getUserAccount().getEmail(), initials(profile.getStageName()),
			profile.getAvatarUrl(), assignment.isActive()
		);
	}

	private UserAccount requireOwner(String email) {
		return userAccountRepository.findByEmailIgnoreCase(email)
			.filter(account -> account.getRole() == UserRole.OWNER && account.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Owner account not found."));
	}

	private Store requireOwnedStore(UserAccount owner, Long storeId) {
		return storeRepository.findById(storeId)
			.filter(store -> store.getOwner().getId().equals(owner.getId()))
			.orElseThrow(() -> new DomainRuleViolationException("Store not found."));
	}

	private String cleanRequired(String value, String field, int maximumLength) {
		var clean = value == null ? "" : value.trim();
		if (clean.isEmpty()) throw new DomainRuleViolationException(field + " is required.");
		if (clean.length() > maximumLength) throw new DomainRuleViolationException(field + " is too long.");
		return clean;
	}

	private String cleanOptional(String value, String field, int maximumLength) {
		if (value == null || value.isBlank()) return null;
		return cleanRequired(value, field, maximumLength);
	}

	private String validTimezone(String value) {
		var clean = cleanRequired(value, "Timezone", 64);
		try {
			return ZoneId.of(clean).getId();
		} catch (DateTimeException exception) {
			throw new DomainRuleViolationException("Timezone must be a valid IANA time zone.");
		}
	}

	private String initials(String name) {
		return Arrays.stream(name.trim().split("\\s+"))
			.filter(part -> !part.isEmpty()).limit(2)
			.map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH))
			.collect(Collectors.joining());
	}
}
