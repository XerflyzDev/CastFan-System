package com.castfan.service;

import com.castfan.entity.CastContent;
import com.castfan.entity.ContentAudience;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastFollowRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentAccessService {
	private final UserAccountRepository userAccountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final CastFollowRepository castFollowRepository;
	private final MembershipService membershipService;

	public ContentAccessService(UserAccountRepository userAccountRepository,
		MemberProfileRepository memberProfileRepository, CastFollowRepository castFollowRepository,
		MembershipService membershipService) {
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.castFollowRepository = castFollowRepository;
		this.membershipService = membershipService;
	}

	@Transactional(readOnly = true)
	public Access access(CastContent content, String authenticatedEmail) {
		if (authenticatedEmail != null
			&& content.getCastProfile().getUserAccount().getEmail().equalsIgnoreCase(authenticatedEmail)) {
			return new Access(true, true, Integer.MAX_VALUE);
		}
		if (content.getAudience() == ContentAudience.PUBLIC) return new Access(true, false, 0);
		if (authenticatedEmail == null) return new Access(false, false, 0);
		var member = userAccountRepository.findByEmailIgnoreCase(authenticatedEmail)
			.filter(account -> (account.getRole() == UserRole.MEMBER || account.getRole() == UserRole.CAST)
				&& account.getStatus() == UserStatus.ACTIVE)
			.flatMap(account -> memberProfileRepository.findByUserAccountId(account.getId()));
		if (member.isEmpty()) return new Access(false, false, 0);
		var followed = castFollowRepository.existsByMemberProfileIdAndCastProfileId(
			member.get().getId(), content.getCastProfile().getId()
		);
		var rank = member.get().getStoreMembershipTier().getRank();
		var accessible = content.getAudience() == ContentAudience.FOLLOWERS
			? followed
			: content.getAudience() == ContentAudience.MEMBERS
				&& membershipService.hasStoreTierAccess(authenticatedEmail, content.getMinimumStoreTierRank());
		return new Access(accessible, followed, rank);
	}

	public String audienceLabel(CastContent content) {
		return switch (content.getAudience()) {
			case PUBLIC -> "Public";
			case FOLLOWERS -> "Followers";
			case MEMBERS -> "Tier " + content.getMinimumStoreTierRank() + "+ members";
		};
	}

	public record Access(boolean accessible, boolean followed, int memberRank) { }
}
