package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.MemberProfileForm;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

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

	@GetMapping("/member/notification-settings")
	@PreAuthorize("hasRole('MEMBER')")
	public String notificationSettings(Principal principal, Model model) {
		model.addAttribute("form", profileService.memberProfileForm(principal.getName()));
		return "member/notification-settings";
	}

	@PostMapping("/member/profile")
	public String updateProfile(Principal principal, @Valid @ModelAttribute("form") MemberProfileForm form,
		BindingResult bindingResult, @RequestParam(name = "image", required = false) MultipartFile image, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				profileService.updateMemberProfile(principal.getName(), form, image);
				return "redirect:/member/profile?saved";
			} catch (DomainRuleViolationException exception) {
				bindingResult.reject("profile.invalid", exception.getMessage());
			}
		}
		model.addAttribute("profile", profileService.memberProfile(principal.getName()));
		return "member/profile";
	}

	@PostMapping("/member/profile/notification-preferences")
	@PreAuthorize("hasRole('MEMBER')")
	@ResponseBody
	public ResponseEntity<Void> updateNotificationPreference(
		Principal principal,
		@RequestParam("preference") String preference,
		@RequestParam("enabled") boolean enabled
	) {
		try {
			profileService.updateMemberNotificationPreference(principal.getName(), preference, enabled);
			return ResponseEntity.noContent().build();
		} catch (DomainRuleViolationException exception) {
			return ResponseEntity.badRequest().build();
		}
	}
}
