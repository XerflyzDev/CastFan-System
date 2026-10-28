package com.castfan.dto;

public record OwnerMemberSearchResult(Long id, String displayName, String email, long pointsBalance,
	String tier) {
}
