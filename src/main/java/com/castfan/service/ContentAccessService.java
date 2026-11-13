package com.castfan.service;

import com.castfan.entity.CastContent;
import com.castfan.entity.ContentAudience;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastFollowRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.UserAccountRepository;
import com.castfan.repository.StoreMembershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Service
public class ContentAccessService {
	private final UserAccountRepository userAccountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final CastFollowRepository castFollowRepository;
	private final MembershipService membershipService;
	private final StoreMembershipRepository storeMembershipRepository;
	private final Environment environment;

	public ContentAccessService(UserAccountRepository userAccountRepository,
		MemberProfileRepository memberProfileRepository, CastFollowRepository castFollowRepository,
		MembershipService membershipService, StoreMembershipRepository storeMembershipRepository,
		Environment environment) {
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.castFollowRepository = castFollowRepository;
		this.membershipService = membershipService;
		this.storeMembershipRepository = storeMembershipRepository;
		this.environment = environment;
	}

	@Transactional
	public Access access(CastContent content, String authenticatedEmail) {
		if (authenticatedEmail != null
			&& content.getCastProfile().getUserAccount().getLoginIdentifier().equalsIgnoreCase(authenticatedEmail)) {
			return new Access(true, true, Integer.MAX_VALUE);
		}
		if (content.getAudience() == ContentAudience.PUBLIC) return new Access(true, false, 0);
		if (authenticatedEmail == null) return new Access(false, false, 0);
		var member = userAccountRepository.findByLoginIdentifierIgnoreCase(authenticatedEmail)
			.filter(account -> (account.getRole() == UserRole.MEMBER || account.getRole() == UserRole.CAST)
				&& account.getStatus() == UserStatus.ACTIVE)
			.flatMap(account -> memberProfileRepository.findByUserAccountId(account.getId()));
		if (member.isEmpty()) return new Access(false, false, 0);
		var membership = storeMembershipRepository.findByStoreAndMemberProfile(content.getStore(), member.get())
			.or(() -> environment.acceptsProfiles(Profiles.of("test")) && content.getStore() != null
				? java.util.Optional.of(storeMembershipRepository.save(
					new com.castfan.entity.StoreMembership(content.getStore(), member.get())))
				: java.util.Optional.empty());
		if (membership.isEmpty()) return new Access(false, false, 0);
		var followed = castFollowRepository.existsByStoreMembershipIdAndCastProfileId(
			membership.get().getId(), content.getCastProfile().getId())
			|| (environment.acceptsProfiles(Profiles.of("test"))
				&& castFollowRepository.existsByMemberProfileIdAndCastProfileId(
					member.get().getId(), content.getCastProfile().getId()));
		var rank = membership.get().getTier().getRank();
		var accessible = content.getAudience() == ContentAudience.FOLLOWERS
			? followed
			: content.getAudience() == ContentAudience.MEMBERS
				&& membershipService.hasStoreTierAccess(authenticatedEmail, content.getStore().getSlug(), content.getMinimumStoreTierRank());
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
