package com.castfan.dto;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.castfan.entity.CastProfile;

public record MemberCastView(
	Long id,
	String stageName,
	String bio,
	String avatarUrl,
	boolean following
) {

	public static MemberCastView from(CastProfile profile, boolean following) {
		return new MemberCastView(
			profile.getId(),
			profile.getStageName(),
			profile.getBio(),
			profile.getAvatarUrl(),
			following
		);
	}

	public String initials() {
		return Arrays.stream(stageName.trim().split("\\s+"))
			.filter(part -> !part.isEmpty())
			.limit(2)
			.map(part -> part.substring(0, 1).toUpperCase())
			.collect(Collectors.joining());
	}
}
