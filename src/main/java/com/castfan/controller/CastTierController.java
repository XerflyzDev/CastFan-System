package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.MemberTierForm;
import com.castfan.service.DomainConflictException;
import com.castfan.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@PreAuthorize("hasRole('CAST')")
public class CastTierController {

	private final MembershipService membershipService;

	public CastTierController(MembershipService membershipService) {
		this.membershipService = membershipService;
	}

	@GetMapping("/cast/tiers")
	public String tiers(Principal principal, Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new MemberTierForm());
		}
		model.addAttribute("tiers", membershipService.ownedTiers(principal.getName()));
		return "cast/tiers";
	}

	@PostMapping("/cast/tiers")
	public String createTier(
		Principal principal,
		@Valid @ModelAttribute("form") MemberTierForm form,
		BindingResult bindingResult,
		Model model
	) {
		if (!bindingResult.hasErrors()) {
			try {
				membershipService.createTier(principal.getName(), form);
				return "redirect:/cast/tiers?created";
			} catch (DomainConflictException exception) {
				bindingResult.reject("tier.conflict", exception.getMessage());
			}
		}

		model.addAttribute("tiers", membershipService.ownedTiers(principal.getName()));
		return "cast/tiers";
	}

	@PostMapping("/cast/tiers/{tierId}/deactivate")
	public String deactivate(Principal principal, @PathVariable Long tierId) {
		membershipService.deactivateTier(principal.getName(), tierId);
		return "redirect:/cast/tiers?deactivated";
	}
}
