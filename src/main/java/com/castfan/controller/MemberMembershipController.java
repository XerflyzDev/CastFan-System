package com.castfan.controller;

import java.security.Principal;

import com.castfan.service.MembershipService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('MEMBER')")
public class MemberMembershipController {

	private final MembershipService membershipService;

	public MemberMembershipController(MembershipService membershipService) {
		this.membershipService = membershipService;
	}

	@GetMapping("/member/memberships")
	public String memberships(Principal principal, Model model) {
		model.addAttribute("points", membershipService.memberPoints(principal.getName()));
		return "member/memberships";
	}
}
