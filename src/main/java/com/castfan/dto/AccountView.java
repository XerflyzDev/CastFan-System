package com.castfan.dto;

import com.castfan.entity.UserAccount;

public record AccountView(String displayName, String email, String role) {

	public static AccountView from(UserAccount account) {
		return new AccountView(account.getDisplayName(), account.getEmail(), account.getRole().name());
	}
}
