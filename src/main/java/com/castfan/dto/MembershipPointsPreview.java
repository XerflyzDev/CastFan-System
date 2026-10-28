package com.castfan.dto;

import com.castfan.entity.StoreMembershipTier;

public record MembershipPointsPreview(
	Long memberProfileId,
	String memberDisplayName,
	long currentBalance,
	long pointsDelta,
	long resultingBalance,
	StoreMembershipTier currentTier,
	StoreMembershipTier resultingTier
) {
}
