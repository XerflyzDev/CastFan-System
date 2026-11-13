package com.castfan.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;
import java.security.SecureRandom;
import java.text.Normalizer;

import com.castfan.dto.OwnerCastAssignmentForm;
import com.castfan.dto.OwnerCastPromotionForm;
import com.castfan.dto.OwnerStoreView;
import com.castfan.dto.OwnerCastCreateForm;
import com.castfan.entity.StoreCast;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerStoreService {
	private final UserAccountRepository userAccountRepository;
	private final StoreCastRepository storeCastRepository;
	private final CastProfileRepository castProfileRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final SystemStoreService systemStoreService;
	private final ProfileService profileService;
	private final PasswordEncoder passwordEncoder;
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final char[] TEMP_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%".toCharArray();

	public OwnerStoreService(UserAccountRepository userAccountRepository, StoreCastRepository storeCastRepository,
		CastProfileRepository castProfileRepository, MemberProfileRepository memberProfileRepository,
		SystemStoreService systemStoreService, ProfileService profileService, PasswordEncoder passwordEncoder) {
		this.userAccountRepository = userAccountRepository;
		this.storeCastRepository = storeCastRepository;
		this.castProfileRepository = castProfileRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.systemStoreService = systemStoreService;
		this.profileService = profileService;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public CastCredentials createCast(String ownerIdentifier, OwnerCastCreateForm form) {
		var store = systemStoreService.requireForOwner(requireOwner(ownerIdentifier));
		var stageName = form.getStageName().trim();
		var stageKey = Normalizer.normalize(stageName, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
		if (castProfileRepository.existsByStoreIdAndStageNameKey(store.getId(), stageKey)) {
			throw new DomainConflictException("This stage name is already used in your store.");
		}
		var base = slugPart(stageName) + "@" + store.getSlug();
		var castId = base;
		for (int suffix = 2; userAccountRepository.existsByLoginIdentifierIgnoreCase(castId); suffix++) {
			castId = base + suffix;
		}
		var temporaryPassword = temporaryPassword();
		var account = userAccountRepository.save(new UserAccount(null, castId,
			passwordEncoder.encode(temporaryPassword), form.getDisplayName().trim(),
			UserRole.CAST, UserStatus.ACTIVE, true));
		var castProfile = profileService.createCastProfile(account, store, stageName, null, null);
		storeCastRepository.saveAndFlush(new StoreCast(store, castProfile, true));
		return new CastCredentials(castId, temporaryPassword);
	}

	@Transactional
	public CastCredentials resetCastPassword(String ownerIdentifier, Long assignmentId) {
		var store = systemStoreService.requireForOwner(requireOwner(ownerIdentifier));
		var assignment = storeCastRepository.findByIdForUpdate(assignmentId)
			.filter(value -> value.getStore().getId().equals(store.getId()))
			.orElseThrow(() -> new DomainRuleViolationException("Cast roster assignment not found."));
		var account = assignment.getCastProfile().getUserAccount();
		var temporaryPassword = temporaryPassword();
		account.resetTemporaryPassword(passwordEncoder.encode(temporaryPassword));
		account.activate();
		userAccountRepository.saveAndFlush(account);
		return new CastCredentials(account.getLoginIdentifier(), temporaryPassword);
	}

	@Transactional(readOnly = true)
	public OwnerStoreView load(String email) {
		var owner = requireOwner(email);
		var systemStore = systemStoreService.requireForOwner(owner);
		var assignments = storeCastRepository.findAllForOwner(email).stream()
			.filter(assignment -> assignment.getStore().getId().equals(systemStore.getId())).toList();
		return new OwnerStoreView(
			java.util.List.of(),
			java.util.List.of(),
			assignments.stream().map(this::toAssignmentItem).toList());
	}

	@Transactional
	public void promoteMember(String email, OwnerCastPromotionForm form) {
		var store = systemStoreService.requireForOwner(requireOwner(email));
		var memberProfile = memberProfileRepository.findByIdForUpdate(form.getMemberProfileId())
			.orElseThrow(() -> new DomainRuleViolationException("Member profile not found."));
		var account = memberProfile.getUserAccount();
		if (account.getRole() != UserRole.MEMBER || account.getStatus() != UserStatus.ACTIVE) {
			throw new DomainRuleViolationException("Only an active Member account can be promoted to Cast.");
		}
		if (castProfileRepository.existsByUserAccountId(account.getId())) {
			throw new DomainConflictException("This account already has a Cast profile.");
		}

		account.promoteMemberToCast();
		userAccountRepository.save(account);
		var castProfile = profileService.createCastProfile(
			account, store, form.getStageName(), memberProfile.getBio(), memberProfile.getAvatarUrl()
		);
		storeCastRepository.saveAndFlush(new StoreCast(store, castProfile, true));
	}

	@Transactional
	public void assignCast(String email, OwnerCastAssignmentForm form) {
		var store = systemStoreService.requireForOwner(requireOwner(email));
		var profile = castProfileRepository.findById(form.getCastProfileId())
			.filter(value -> value.getUserAccount().getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Active Cast profile not found."));
		var existing = storeCastRepository.findByStoreIdAndCastProfileId(store.getId(), profile.getId());
		if (existing.isPresent()) {
			var assignment = existing.get();
			if (assignment.isActive()) throw new DomainConflictException("This Cast is already on the roster.");
			assignment.activate();
			storeCastRepository.saveAndFlush(assignment);
			return;
		}
		storeCastRepository.saveAndFlush(new StoreCast(store, profile, true));
	}

	@Transactional
	public void setAssignmentActive(String email, Long assignmentId, boolean active) {
		var store = systemStoreService.requireForOwner(requireOwner(email));
		var assignment = storeCastRepository.findByIdForUpdate(assignmentId)
			.filter(value -> value.getStore().getId().equals(store.getId()))
			.orElseThrow(() -> new DomainRuleViolationException("Cast roster assignment not found."));
		if (active) {
			assignment.activate();
			assignment.getCastProfile().getUserAccount().activate();
		} else {
			assignment.deactivate();
			assignment.getCastProfile().getUserAccount().suspend();
		}
		storeCastRepository.saveAndFlush(assignment);
	}

	private OwnerStoreView.AssignmentItem toAssignmentItem(StoreCast assignment) {
		var profile = assignment.getCastProfile();
		return new OwnerStoreView.AssignmentItem(assignment.getId(), profile.getId(), profile.getStageName(),
			profile.getUserAccount().getLoginIdentifier(), initials(profile.getStageName()), profile.getAvatarUrl(), assignment.isActive());
	}

	private UserAccount requireOwner(String email) {
		return userAccountRepository.findByLoginIdentifierIgnoreCase(email)
			.filter(account -> account.getRole() == UserRole.OWNER && account.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Owner account not found."));
	}

	private String initials(String name) {
		return Arrays.stream(name.trim().split("\\s+")).filter(part -> !part.isEmpty()).limit(2)
			.map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH)).collect(Collectors.joining());
	}

	private String slugPart(String value) {
		var key = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
			.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
		return key.isBlank() ? "cast" : key;
	}

	private String temporaryPassword() {
		var result = new StringBuilder(16);
		for (int index = 0; index < 16; index++) result.append(TEMP_ALPHABET[RANDOM.nextInt(TEMP_ALPHABET.length)]);
		return result.toString();
	}

	public record CastCredentials(String castId, String temporaryPassword) { }
}
