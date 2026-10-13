package com.castfan.controller;

import java.security.Principal;

import com.castfan.service.DomainConflictException;
import com.castfan.service.MembershipService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('MEMBER')")
public class MemberMembershipController {

	private final MembershipService membershipService;

	public MemberMembershipController(MembershipService membershipService) {
		this.membershipService = membershipService;
	}

	@GetMapping("/member/memberships")
	public String memberships(Principal principal, Model model) {
		model.addAttribute("memberships", membershipService.activeMemberships(principal.getName()));
		model.addAttribute("availableTiers", membershipService.availableTiers(principal.getName()));
		return "member/memberships";
	}

	@PostMapping("/member/tiers/{tierId}/subscribe")
	public String subscribe(
		Principal principal,
		@PathVariable Long tierId,
		RedirectAttributes redirectAttributes
	) {
		try {
			membershipService.subscribe(principal.getName(), tierId);
			redirectAttributes.addFlashAttribute("membershipSuccess", "Membership activated.");
		} catch (DomainConflictException exception) {
			redirectAttributes.addFlashAttribute("membershipError", exception.getMessage());
		}
		return "redirect:/member/memberships";
	}

	@PostMapping("/member/memberships/{membershipId}/cancel")
	public String cancel(Principal principal, @PathVariable Long membershipId) {
		membershipService.cancel(principal.getName(), membershipId);
		return "redirect:/member/memberships?cancelled";
	}
}
