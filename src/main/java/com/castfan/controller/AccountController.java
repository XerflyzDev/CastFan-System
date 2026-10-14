package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.AccountView;
import com.castfan.entity.UserRole;
import com.castfan.repository.UserAccountRepository;
import com.castfan.service.MembershipService;
import com.castfan.service.ProfileService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {

	private final UserAccountRepository userAccountRepository;
	private final MembershipService membershipService;
	private final ProfileService profileService;

	public AccountController(UserAccountRepository userAccountRepository, MembershipService membershipService,
		ProfileService profileService) {
		this.userAccountRepository = userAccountRepository;
		this.membershipService = membershipService;
		this.profileService = profileService;
	}

	@GetMapping("/account")
	public String account(Principal principal, Model model) {
		var account = userAccountRepository.findByEmailIgnoreCase(principal.getName())
			.orElseThrow(() -> new UsernameNotFoundException("Account not found"));
		model.addAttribute("account", AccountView.from(account));
		model.addAttribute(
			"memberships",
			account.getRole() == UserRole.MEMBER
				? membershipService.activeMemberships(account.getEmail())
				: java.util.List.of()
		);
		model.addAttribute(
			"memberProfile",
			account.getRole() == UserRole.MEMBER ? profileService.memberProfile(account.getEmail()) : null
		);
		return "account/index";
	}
}
