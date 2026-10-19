package com.castfan.controller;

import java.security.Principal;

import com.castfan.service.MemberCastService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasAnyRole('MEMBER', 'CAST')")
public class MemberCastController {

	private static final int PAGE_SIZE = 12;

	private final MemberCastService memberCastService;

	public MemberCastController(MemberCastService memberCastService) {
		this.memberCastService = memberCastService;
	}

	@GetMapping("/member/casts")
	public String discover(
		Principal principal,
		@RequestParam(defaultValue = "0") int page,
		Model model
	) {
		var safePage = Math.max(page, 0);
		model.addAttribute("castPage", memberCastService.discover(principal.getName(), PageRequest.of(safePage, PAGE_SIZE)));
		return "member/casts";
	}

	@GetMapping("/member/following")
	public String following(Principal principal, Model model) {
		model.addAttribute("castList", memberCastService.following(principal.getName()));
		return "member/following";
	}

	@PostMapping("/member/casts/{castProfileId}/follow")
	public String follow(Principal principal, @PathVariable Long castProfileId) {
		memberCastService.follow(principal.getName(), castProfileId);
		return "redirect:/member/casts";
	}

	@PostMapping("/member/casts/{castProfileId}/unfollow")
	public String unfollow(Principal principal, @PathVariable Long castProfileId) {
		memberCastService.unfollow(principal.getName(), castProfileId);
		return "redirect:/member/following";
	}

	@PostMapping("/member/casts/{castProfileId}/follow-profile")
	public String followFromProfile(Principal principal, @PathVariable Long castProfileId) {
		memberCastService.follow(principal.getName(), castProfileId);
		return "redirect:/casts/" + castProfileId;
	}

	@PostMapping("/member/casts/{castProfileId}/unfollow-profile")
	public String unfollowFromProfile(Principal principal, @PathVariable Long castProfileId) {
		memberCastService.unfollow(principal.getName(), castProfileId);
		return "redirect:/casts/" + castProfileId;
	}
}
