package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.AccountView;
import com.castfan.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {

	private final UserAccountRepository userAccountRepository;

	public AccountController(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@GetMapping("/account")
	public String account(Principal principal, Model model) {
		var account = userAccountRepository.findByEmailIgnoreCase(principal.getName())
			.orElseThrow(() -> new UsernameNotFoundException("Account not found"));
		model.addAttribute("account", AccountView.from(account));
		return "account/index";
	}
}
