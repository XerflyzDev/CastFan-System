package com.castfan.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.castfan.entity.TierMembership;

public record MembershipView(
	Long id,
	Long tierId,
	String castStageName,
	String tierName,
	int rank,
	BigDecimal monthlyPrice,
	String currency,
	String status,
	Instant startedAt
) {

	public static MembershipView from(TierMembership membership) {
		var tier = membership.getMemberTier();
		return new MembershipView(
			membership.getId(),
			tier.getId(),
			tier.getCastProfile().getStageName(),
			tier.getName(),
			tier.getRank(),
			tier.getMonthlyPrice(),
			tier.getCurrency(),
			membership.getStatus().name(),
			membership.getStartedAt()
		);
	}
}
