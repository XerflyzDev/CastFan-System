package com.castfan.service;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import com.castfan.dto.OwnerCastAssignmentForm;
import com.castfan.dto.OwnerCastPromotionForm;
import com.castfan.dto.OwnerStoreView;
import com.castfan.entity.StoreCast;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OwnerStoreService {
	private final UserAccountRepository userAccountRepository;
	private final StoreCastRepository storeCastRepository;
	private final CastProfileRepository castProfileRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final SystemStoreService systemStoreService;
	private final ProfileService profileService;

	public OwnerStoreService(UserAccountRepository userAccountRepository, StoreCastRepository storeCastRepository,
		CastProfileRepository castProfileRepository, MemberProfileRepository memberProfileRepository,
		SystemStoreService systemStoreService, ProfileService profileService) {
		this.userAccountRepository = userAccountRepository;
		this.storeCastRepository = storeCastRepository;
		this.castProfileRepository = castProfileRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.systemStoreService = systemStoreService;
		this.profileService = profileService;
	}

	@Transactional(readOnly = true)
	public OwnerStoreView load(String email) {
		var owner = requireOwner(email);
		var systemStore = systemStoreService.requireForOwner(owner);
		var assignments = storeCastRepository.findAllForOwner(email).stream()
			.filter(assignment -> assignment.getStore().getId().equals(systemStore.getId())).toList();
		return new OwnerStoreView(
			castProfileRepository.findAllByUserAccountStatusOrderByStageNameAsc(UserStatus.ACTIVE).stream()
				.map(profile -> new OwnerStoreView.CastOption(profile.getId(), profile.getStageName(),
					profile.getUserAccount().getEmail())).toList(),
			memberProfileRepository.findCastPromotionCandidates(UserRole.MEMBER, UserStatus.ACTIVE).stream()
				.map(profile -> new OwnerStoreView.MemberOption(
					profile.getId(), profile.getUserAccount().getDisplayName(), profile.getUserAccount().getEmail()
				)).toList(),
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
			account, form.getStageName(), memberProfile.getBio(), memberProfile.getAvatarUrl()
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
		if (active) assignment.activate(); else assignment.deactivate();
		storeCastRepository.saveAndFlush(assignment);
	}

	private OwnerStoreView.AssignmentItem toAssignmentItem(StoreCast assignment) {
		var profile = assignment.getCastProfile();
		return new OwnerStoreView.AssignmentItem(assignment.getId(), profile.getId(), profile.getStageName(),
			profile.getUserAccount().getEmail(), initials(profile.getStageName()), profile.getAvatarUrl(), assignment.isActive());
	}

	private UserAccount requireOwner(String email) {
		return userAccountRepository.findByEmailIgnoreCase(email)
			.filter(account -> account.getRole() == UserRole.OWNER && account.getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Owner account not found."));
	}

	private String initials(String name) {
		return Arrays.stream(name.trim().split("\\s+")).filter(part -> !part.isEmpty()).limit(2)
			.map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH)).collect(Collectors.joining());
	}
}
