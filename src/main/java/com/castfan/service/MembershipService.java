package com.castfan.service;

import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.castfan.dto.MemberTierForm;
import com.castfan.dto.MemberTierView;
import com.castfan.dto.MembershipView;
import com.castfan.entity.CastProfile;
import com.castfan.entity.MemberProfile;
import com.castfan.entity.MemberTier;
import com.castfan.entity.MembershipStatus;
import com.castfan.entity.TierMembership;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.MemberTierRepository;
import com.castfan.repository.TierMembershipRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

	private static final int AVAILABLE_TIER_LIMIT = 50;

	private final UserAccountRepository userAccountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final CastProfileRepository castProfileRepository;
	private final MemberTierRepository memberTierRepository;
	private final TierMembershipRepository tierMembershipRepository;

	public MembershipService(
		UserAccountRepository userAccountRepository,
		MemberProfileRepository memberProfileRepository,
		CastProfileRepository castProfileRepository,
		MemberTierRepository memberTierRepository,
		TierMembershipRepository tierMembershipRepository
	) {
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.castProfileRepository = castProfileRepository;
		this.memberTierRepository = memberTierRepository;
		this.tierMembershipRepository = tierMembershipRepository;
	}

	@Transactional
	public MemberTierView createTier(String authenticatedEmail, MemberTierForm form) {
		var castProfile = requireActiveCast(authenticatedEmail);
		validateTierForm(form);
		var name = form.getName().trim();
		var nameKey = Normalizer.normalize(name, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
		var rank = form.getRank();
		var price = form.getMonthlyPrice().setScale(2, RoundingMode.UNNECESSARY);

		if (memberTierRepository.existsByCastProfileIdAndNameKey(castProfile.getId(), nameKey)) {
			throw new DomainConflictException("This tier name is already in use.");
		}
		if (memberTierRepository.existsByCastProfileIdAndRank(castProfile.getId(), rank)) {
			throw new DomainConflictException("This tier rank is already in use.");
		}

		var tier = new MemberTier(
			castProfile,
			name,
			nameKey,
			cleanOptional(form.getDescription()),
			rank,
			price,
			"THB"
		);

		try {
			return MemberTierView.owned(memberTierRepository.saveAndFlush(tier));
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("This tier name or rank is already in use.");
		}
	}

	@Transactional(readOnly = true)
	public List<MemberTierView> ownedTiers(String authenticatedEmail) {
		var castProfile = requireActiveCast(authenticatedEmail);
		return memberTierRepository.findAllByCastProfileIdOrderByRankAsc(castProfile.getId()).stream()
			.map(MemberTierView::owned)
			.toList();
	}

	@Transactional
	public void deactivateTier(String authenticatedEmail, Long tierId) {
		var castProfile = requireActiveCast(authenticatedEmail);
		var tier = requireTier(tierId);
		if (!tier.getCastProfile().getId().equals(castProfile.getId())) {
			throw new DomainRuleViolationException("Cast can manage only their own tiers.");
		}
		tier.deactivate();
		memberTierRepository.saveAndFlush(tier);
	}

	@Transactional(readOnly = true)
	public List<MemberTierView> availableTiers(String authenticatedEmail) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		var activeTierByCast = new HashMap<Long, Long>();
		for (var membership : tierMembershipRepository.findAllActiveForMember(
			memberProfile.getId(), MembershipStatus.ACTIVE
		)) {
			var tier = membership.getMemberTier();
			activeTierByCast.put(tier.getCastProfile().getId(), tier.getId());
		}

		return memberTierRepository.findAvailable(UserStatus.ACTIVE, PageRequest.of(0, AVAILABLE_TIER_LIMIT)).stream()
			.map(tier -> MemberTierView.from(tier, activeTierByCast.get(tier.getCastProfile().getId())))
			.toList();
	}

	@Transactional(readOnly = true)
	public List<MembershipView> activeMemberships(String authenticatedEmail) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		return tierMembershipRepository.findAllActiveForMember(memberProfile.getId(), MembershipStatus.ACTIVE).stream()
			.map(MembershipView::from)
			.toList();
	}

	@Transactional
	public MembershipView subscribe(String authenticatedEmail, Long tierId) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		memberProfileRepository.findByIdForUpdate(memberProfile.getId())
			.orElseThrow(() -> new DomainRuleViolationException("Member profile not found."));
		var tier = requireTier(tierId);
		if (!tier.isActive()
			|| !tier.getCastProfile().isDiscoverable()
			|| tier.getCastProfile().getUserAccount().getStatus() != UserStatus.ACTIVE) {
			throw new DomainRuleViolationException("Membership tier is not available.");
		}

		var activeForCast = tierMembershipRepository.findActiveForCast(
			memberProfile.getId(), tier.getCastProfile().getId(), MembershipStatus.ACTIVE
		);
		if (activeForCast.isPresent()) {
			if (activeForCast.get().getMemberTier().getId().equals(tierId)) {
				return MembershipView.from(activeForCast.get());
			}
			throw new DomainConflictException("Cancel the current tier before joining another tier for this Cast.");
		}

		var membership = tierMembershipRepository.findByMemberProfileIdAndMemberTierId(memberProfile.getId(), tierId)
			.orElseGet(() -> new TierMembership(memberProfile, tier));
		membership.reactivate();
		return MembershipView.from(tierMembershipRepository.saveAndFlush(membership));
	}

	@Transactional
	public void cancel(String authenticatedEmail, Long membershipId) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		var membership = tierMembershipRepository.findByIdForUpdate(membershipId)
			.orElseThrow(() -> new DomainRuleViolationException("Membership not found."));
		if (!membership.getMemberProfile().getId().equals(memberProfile.getId())) {
			throw new DomainRuleViolationException("Member can cancel only their own membership.");
		}
		try {
			membership.cancel();
		} catch (IllegalStateException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
		tierMembershipRepository.saveAndFlush(membership);
	}

	@Transactional(readOnly = true)
	public boolean hasTierAccess(String authenticatedEmail, Long castProfileId, int minimumRank) {
		if (minimumRank < 1) {
			throw new DomainRuleViolationException("Minimum tier rank must be positive.");
		}
		var memberProfile = requireActiveMember(authenticatedEmail);
		return tierMembershipRepository.findActiveForCast(
			memberProfile.getId(), castProfileId, MembershipStatus.ACTIVE
		).map(membership -> membership.getMemberTier().getRank() >= minimumRank)
			.orElse(false);
	}

	private MemberTier requireTier(Long tierId) {
		return memberTierRepository.findById(tierId)
			.orElseThrow(() -> new DomainRuleViolationException("Membership tier not found."));
	}

	private MemberProfile requireActiveMember(String email) {
		var account = requireActiveAccount(email, UserRole.MEMBER);
		return memberProfileRepository.findByUserAccountId(account.getId())
			.orElseThrow(() -> new DomainRuleViolationException("Member profile not found."));
	}

	private CastProfile requireActiveCast(String email) {
		var account = requireActiveAccount(email, UserRole.CAST);
		return castProfileRepository.findByUserAccountId(account.getId())
			.orElseThrow(() -> new DomainRuleViolationException("Cast profile not found."));
	}

	private UserAccount requireActiveAccount(String email, UserRole role) {
		var account = userAccountRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new DomainRuleViolationException("Account not found."));
		if (account.getRole() != role || account.getStatus() != UserStatus.ACTIVE) {
			throw new DomainRuleViolationException("An active " + role.name() + " account is required.");
		}
		return account;
	}

	private String cleanOptional(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private void validateTierForm(MemberTierForm form) {
		if (form == null) {
			throw new DomainRuleViolationException("Membership tier details are required.");
		}
		var name = form.getName() == null ? "" : form.getName().trim();
		if (name.length() < 2 || name.length() > 80) {
			throw new DomainRuleViolationException("Tier name must be between 2 and 80 characters.");
		}
		if (form.getDescription() != null && form.getDescription().trim().length() > 500) {
			throw new DomainRuleViolationException("Description must not exceed 500 characters.");
		}
		if (form.getRank() == null || form.getRank() < 1 || form.getRank() > 100) {
			throw new DomainRuleViolationException("Rank must be between 1 and 100.");
		}
		if (form.getMonthlyPrice() == null
			|| form.getMonthlyPrice().signum() < 0
			|| form.getMonthlyPrice().scale() > 2
			|| form.getMonthlyPrice().precision() - form.getMonthlyPrice().scale() > 8) {
			throw new DomainRuleViolationException("Monthly price must be non-negative with up to 8 digits and 2 decimal places.");
		}
	}
}
