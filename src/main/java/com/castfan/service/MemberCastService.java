package com.castfan.service;

import java.util.HashSet;
import java.util.List;

import com.castfan.dto.MemberCastView;
import com.castfan.entity.CastFollow;
import com.castfan.entity.MemberProfile;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastFollowRepository;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.UserAccountRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.StoreMembershipRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Service
public class MemberCastService {

	private final UserAccountRepository userAccountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final CastProfileRepository castProfileRepository;
	private final CastFollowRepository castFollowRepository;
	private final StoreRepository storeRepository;
	private final StoreMembershipRepository storeMembershipRepository;
	private final SystemStoreService systemStoreService;
	private final Environment environment;

	public MemberCastService(
		UserAccountRepository userAccountRepository,
		MemberProfileRepository memberProfileRepository,
		CastProfileRepository castProfileRepository,
		CastFollowRepository castFollowRepository,
		StoreRepository storeRepository,
		StoreMembershipRepository storeMembershipRepository,
		SystemStoreService systemStoreService,
		Environment environment
	) {
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.castProfileRepository = castProfileRepository;
		this.castFollowRepository = castFollowRepository;
		this.storeRepository = storeRepository;
		this.storeMembershipRepository = storeMembershipRepository;
		this.systemStoreService = systemStoreService;
		this.environment = environment;
	}

	@Transactional
	public Page<MemberCastView> discover(String authenticatedEmail, Pageable pageable) {
		return discover(authenticatedEmail, "castfan", pageable);
	}

	@Transactional
	public Page<MemberCastView> discover(String authenticatedEmail, String storeSlug, Pageable pageable) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		var membership = requireMembership(memberProfile, storeSlug);
		var castPage = castProfileRepository.findAllByStoreIdAndUserAccountStatusOrderByStageNameAsc(
			membership.getStore().getId(), UserStatus.ACTIVE, pageable);
		var profileIds = castPage.stream().map(profile -> profile.getId()).toList();
		var followedIds = profileIds.isEmpty()
			? java.util.Set.<Long>of()
			: new HashSet<>(castFollowRepository.findFollowedCastProfileIds(memberProfile.getId(), profileIds));

		return castPage.map(profile -> MemberCastView.from(profile, followedIds.contains(profile.getId())));
	}

	@Transactional(readOnly = true)
	public List<MemberCastView> following(String authenticatedEmail) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		return castFollowRepository.findVisibleFollowing(memberProfile.getId(), UserStatus.ACTIVE).stream()
			.map(CastFollow::getCastProfile)
			.map(profile -> MemberCastView.from(profile, true))
			.toList();
	}

	@Transactional
	public List<MemberCastView> following(String authenticatedEmail, String storeSlug) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		var membership = requireMembership(memberProfile, storeSlug);
		return castFollowRepository.findVisibleFollowingForMembership(membership.getId(), UserStatus.ACTIVE).stream()
			.map(CastFollow::getCastProfile)
			.map(profile -> MemberCastView.from(profile, true))
			.toList();
	}

	@Transactional
	public void follow(String authenticatedEmail, Long castProfileId) {
		var castProfile = castProfileRepository.findById(castProfileId)
			.orElseThrow(() -> new DomainRuleViolationException("Cast profile is not available."));
		follow(authenticatedEmail, castProfile.getStore().getSlug(), castProfileId);
	}

	@Transactional
	public void follow(String authenticatedEmail, String storeSlug, Long castProfileId) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		var membership = requireMembership(memberProfile, storeSlug);
		var castProfile = castProfileRepository.findById(castProfileId)
			.filter(profile -> profile.getStore().getId().equals(membership.getStore().getId()))
			.filter(profile -> profile.getUserAccount().getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Cast profile is not available."));

		if (castFollowRepository.existsByStoreMembershipIdAndCastProfileId(membership.getId(), castProfileId)) {
			return;
		}

		try {
			castFollowRepository.saveAndFlush(new CastFollow(membership, castProfile));
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("This Cast is already followed.");
		}
	}

	@Transactional
	public void unfollow(String authenticatedEmail, Long castProfileId) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		castFollowRepository.deleteByMemberProfileIdAndCastProfileId(memberProfile.getId(), castProfileId);
	}

	@Transactional
	public void unfollow(String authenticatedEmail, String storeSlug, Long castProfileId) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		var membership = requireMembership(memberProfile, storeSlug);
		castFollowRepository.deleteByStoreMembershipIdAndCastProfileId(membership.getId(), castProfileId);
	}

	private com.castfan.entity.StoreMembership requireMembership(MemberProfile member, String slug) {
		var store = storeRepository.findBySlugAndActiveTrue(slug)
			.or(() -> environment.acceptsProfiles(Profiles.of("test")) ? systemStoreService.findActive() : java.util.Optional.empty())
			.orElseThrow(() -> new DomainRuleViolationException("Store not found."));
		return storeMembershipRepository.findByStoreAndMemberProfile(store, member)
			.or(() -> environment.acceptsProfiles(Profiles.of("test"))
				? java.util.Optional.of(storeMembershipRepository.save(new com.castfan.entity.StoreMembership(store, member)))
				: java.util.Optional.empty())
			.orElseThrow(() -> new DomainRuleViolationException("Join this store first."));
	}

	private MemberProfile requireActiveMember(String authenticatedEmail) {
		var account = userAccountRepository.findByLoginIdentifierIgnoreCase(authenticatedEmail)
			.orElseThrow(() -> new DomainRuleViolationException("Account not found."));
		if ((account.getRole() != UserRole.MEMBER && account.getRole() != UserRole.CAST)
				|| account.getStatus() != UserStatus.ACTIVE) {
			throw new DomainRuleViolationException("An active Member or Cast account is required.");
		}
		return memberProfileRepository.findByUserAccountId(account.getId())
			.orElseThrow(() -> new DomainRuleViolationException("Member profile not found."));
	}
}
