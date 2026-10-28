package com.castfan.dto;

import com.castfan.entity.StoreMembershipTier;

public record MembershipPointsResult(
	MemberPointTransactionView transaction,
	long pointsBalance,
	StoreMembershipTier tier
) {
}
