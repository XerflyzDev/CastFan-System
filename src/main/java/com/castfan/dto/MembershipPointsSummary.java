package com.castfan.dto;

import java.util.List;

import com.castfan.entity.StoreMembershipTier;

public record MembershipPointsSummary(
	Long memberProfileId,
	String memberDisplayName,
	long pointsBalance,
	StoreMembershipTier tier,
	List<MemberPointTransactionView> transactions
) {
}
