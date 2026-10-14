package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.MemberProfileForm;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class MemberProfileController {

	private final ProfileService profileService;

	public MemberProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping("/member/profile")
	public String profile(Principal principal, Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", profileService.memberProfileForm(principal.getName()));
		}
		model.addAttribute("profile", profileService.memberProfile(principal.getName()));
		return "member/profile";
	}

	@PostMapping("/member/profile")
	public String updateProfile(Principal principal, @Valid @ModelAttribute("form") MemberProfileForm form,
		BindingResult bindingResult, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				profileService.updateMemberProfile(principal.getName(), form);
				return "redirect:/member/profile?saved";
			} catch (DomainRuleViolationException exception) {
				bindingResult.reject("profile.invalid", exception.getMessage());
			}
		}
		model.addAttribute("profile", profileService.memberProfile(principal.getName()));
		return "member/profile";
	}
}
