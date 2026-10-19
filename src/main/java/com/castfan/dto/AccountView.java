package com.castfan.dto;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import com.castfan.entity.UserAccount;

public record AccountView(String displayName, String email, String role, String initials, String joinedLabel,
	boolean hasAvatar) {

	private static final DateTimeFormatter JOINED_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

	public static AccountView from(UserAccount account, boolean hasAvatar) {
		return new AccountView(
			account.getDisplayName(),
			account.getEmail(),
			account.getRole().name(),
			initials(account.getDisplayName()),
			JOINED_FORMAT.format(account.getCreatedAt().atZone(ZoneId.of("Asia/Bangkok"))),
			hasAvatar
		);
	}

	private static String initials(String name) {
		return Arrays.stream(name.trim().split("\\s+"))
			.filter(part -> !part.isEmpty())
			.limit(2)
			.map(part -> part.substring(0, 1).toUpperCase(Locale.ENGLISH))
			.collect(Collectors.joining());
	}
}
