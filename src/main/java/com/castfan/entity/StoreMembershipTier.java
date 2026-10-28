package com.castfan.entity;

public enum StoreMembershipTier {

	NONE(0, 0),
	TIER_1(1, 1),
	TIER_2(2, 5_000),
	TIER_3(3, 20_000);

	private final int rank;
	private final long minimumPoints;

	StoreMembershipTier(int rank, long minimumPoints) {
		this.rank = rank;
		this.minimumPoints = minimumPoints;
	}

	public int getRank() {
		return rank;
	}

	public long getMinimumPoints() {
		return minimumPoints;
	}

	public static StoreMembershipTier fromPoints(long points) {
		if (points < 0) {
			throw new IllegalArgumentException("Points cannot be negative.");
		}
		if (points >= TIER_3.minimumPoints) return TIER_3;
		if (points >= TIER_2.minimumPoints) return TIER_2;
		if (points >= TIER_1.minimumPoints) return TIER_1;
		return NONE;
	}

	public static StoreMembershipTier fromRank(int rank) {
		for (var tier : values()) {
			if (tier.rank == rank) return tier;
		}
		throw new IllegalArgumentException("Store membership tier rank must be between 0 and 3.");
	}
}
