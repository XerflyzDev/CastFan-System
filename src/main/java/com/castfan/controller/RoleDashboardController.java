package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.AccountView;
import com.castfan.repository.UserAccountRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RoleDashboardController {

	private final UserAccountRepository userAccountRepository;

	public RoleDashboardController(UserAccountRepository userAccountRepository) {
		this.userAccountRepository = userAccountRepository;
	}

	@GetMapping("/cast")
	@PreAuthorize("hasRole('CAST')")
	public String castWorkspace(Principal principal, Model model) {
		model.addAttribute("account", currentAccount(principal));
		return "workspace/cast";
	}

	@GetMapping("/owner")
	@PreAuthorize("hasRole('OWNER')")
	public String ownerWorkspace(Principal principal, Model model) {
		model.addAttribute("account", currentAccount(principal));
		return "workspace/owner";
	}

	private AccountView currentAccount(Principal principal) {
		return userAccountRepository.findByEmailIgnoreCase(principal.getName())
			.map(AccountView::from)
			.orElseThrow(() -> new UsernameNotFoundException("Account not found"));
	}
}
