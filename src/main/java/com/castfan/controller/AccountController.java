package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.AccountView;
import com.castfan.entity.UserRole;
import com.castfan.repository.UserAccountRepository;
import com.castfan.service.MembershipService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {

	private final UserAccountRepository userAccountRepository;
	private final MembershipService membershipService;

	public AccountController(UserAccountRepository userAccountRepository, MembershipService membershipService) {
		this.userAccountRepository = userAccountRepository;
		this.membershipService = membershipService;
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
		return "account/index";
	}
}
