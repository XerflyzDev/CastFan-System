package com.castfan.controller;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

import com.castfan.repository.UserAccountRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Validated
public class PasswordChangeController {
	private final UserAccountRepository accounts;
	private final PasswordEncoder passwordEncoder;

	public PasswordChangeController(UserAccountRepository accounts, PasswordEncoder passwordEncoder) {
		this.accounts = accounts; this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/account/change-password")
	public String form() { return "auth/change-password"; }

	@PostMapping("/account/change-password")
	@Transactional
	public String change(Principal principal,
		@RequestParam @NotBlank @Size(min = 8, max = 72) String password,
		@RequestParam String confirmPassword, Model model) {
		if (!password.equals(confirmPassword)) {
			model.addAttribute("error", "Passwords do not match.");
			return "auth/change-password";
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			model.addAttribute("error", "Password must not exceed 72 bytes.");
			return "auth/change-password";
		}
		var account = accounts.findByLoginIdentifierIgnoreCase(principal.getName()).orElseThrow();
		account.changePassword(passwordEncoder.encode(password));
		accounts.saveAndFlush(account);
		return switch (account.getRole()) {
			case CAST -> "redirect:/cast?passwordChanged";
			case OWNER -> "redirect:/owner?passwordChanged";
			case MEMBER -> "redirect:/account/stores?passwordChanged";
		};
	}
}
