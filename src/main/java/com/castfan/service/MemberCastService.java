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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberCastService {

	private final UserAccountRepository userAccountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final CastProfileRepository castProfileRepository;
	private final CastFollowRepository castFollowRepository;

	public MemberCastService(
		UserAccountRepository userAccountRepository,
		MemberProfileRepository memberProfileRepository,
		CastProfileRepository castProfileRepository,
		CastFollowRepository castFollowRepository
	) {
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.castProfileRepository = castProfileRepository;
		this.castFollowRepository = castFollowRepository;
	}

	@Transactional(readOnly = true)
	public Page<MemberCastView> discover(String authenticatedEmail, Pageable pageable) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		var castPage = castProfileRepository.findAllByDiscoverableTrueAndUserAccountStatusOrderByStageNameAsc(
			UserStatus.ACTIVE,
			pageable
		);
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
	public void follow(String authenticatedEmail, Long castProfileId) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		var castProfile = castProfileRepository.findById(castProfileId)
			.filter(profile -> profile.isDiscoverable())
			.filter(profile -> profile.getUserAccount().getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Cast profile is not available."));

		if (castFollowRepository.existsByMemberProfileIdAndCastProfileId(memberProfile.getId(), castProfileId)) {
			return;
		}

		try {
			castFollowRepository.saveAndFlush(new CastFollow(memberProfile, castProfile));
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException("This Cast is already followed.");
		}
	}

	@Transactional
	public void unfollow(String authenticatedEmail, Long castProfileId) {
		var memberProfile = requireActiveMember(authenticatedEmail);
		castFollowRepository.deleteByMemberProfileIdAndCastProfileId(memberProfile.getId(), castProfileId);
	}

	private MemberProfile requireActiveMember(String authenticatedEmail) {
		var account = userAccountRepository.findByEmailIgnoreCase(authenticatedEmail)
			.orElseThrow(() -> new DomainRuleViolationException("Account not found."));
		if (account.getRole() != UserRole.MEMBER || account.getStatus() != UserStatus.ACTIVE) {
			throw new DomainRuleViolationException("An active Member account is required.");
		}
		return memberProfileRepository.findByUserAccountId(account.getId())
			.orElseThrow(() -> new DomainRuleViolationException("Member profile not found."));
	}
}
