package com.castfan.dto;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import com.castfan.entity.MemberProfile;

public record MemberProfileView(Long id, String displayName, String initials, String bio, String avatarUrl,
	boolean castUpdateNotifications, boolean eventNotifications) {

	public static MemberProfileView from(MemberProfile profile) {
		var name = profile.getUserAccount().getDisplayName();
		return new MemberProfileView(profile.getId(), name, initials(name), profile.getBio(), profile.getAvatarUrl(),
			profile.isCastUpdateNotifications(), profile.isEventNotifications());
	}

	private static String initials(String name) {
		return Arrays.stream(name.trim().split("\\s+")).filter(part -> !part.isEmpty()).limit(2)
			.map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH)).collect(Collectors.joining());
	}
}
