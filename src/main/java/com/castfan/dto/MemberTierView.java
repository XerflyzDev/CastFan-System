package com.castfan.dto;

import java.math.BigDecimal;

import com.castfan.entity.MemberTier;

public record MemberTierView(
	Long id,
	Long castProfileId,
	String castStageName,
	String name,
	String description,
	int rank,
	BigDecimal monthlyPrice,
	String currency,
	boolean active,
	boolean current,
	boolean anotherActiveTier
) {

	public static MemberTierView owned(MemberTier tier) {
		return from(tier, null);
	}

	public static MemberTierView from(MemberTier tier, Long currentTierId) {
		var current = currentTierId != null && currentTierId.equals(tier.getId());
		return new MemberTierView(
			tier.getId(),
			tier.getCastProfile().getId(),
			tier.getCastProfile().getStageName(),
			tier.getName(),
			tier.getDescription(),
			tier.getRank(),
			tier.getMonthlyPrice(),
			tier.getCurrency(),
			tier.isActive(),
			current,
			currentTierId != null && !current
		);
	}
}
