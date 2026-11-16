package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.MemberProfileForm;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.ProfileService;
import com.castfan.service.MembershipService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class MemberProfileController {

	private final ProfileService profileService;
	private final MembershipService membershipService;

	public MemberProfileController(ProfileService profileService, MembershipService membershipService) {
		this.profileService = profileService;
		this.membershipService = membershipService;
	}

	@GetMapping("/s/{slug}/notification-settings")
	@PreAuthorize("hasRole('MEMBER')")
	public String storeNotificationSettings(@PathVariable String slug, Principal principal, Model model) {
		model.addAttribute("storeSlug", slug);
		model.addAttribute("form", membershipService.notificationPreferenceForm(principal.getName(), slug));
		return "member/notification-settings";
	}

	@GetMapping("/member/profile")
	public String profile(Principal principal, @RequestParam(name = "store", required = false) String store,
		Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", profileService.memberProfileForm(principal.getName()));
		}
		model.addAttribute("profile", profileService.memberProfile(principal.getName()));
		addStoreContext(principal.getName(), store, model);
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
		BindingResult bindingResult, @RequestParam(name = "image", required = false) MultipartFile image,
		@RequestParam(name = "store", required = false) String store, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				profileService.updateMemberProfile(principal.getName(), form, image);
				var redirect = "redirect:/member/profile?saved";
				if (hasStoreMembership(principal.getName(), store)) {
					redirect += "&store=" + store;
				}
				return redirect;
			} catch (DomainRuleViolationException exception) {
				bindingResult.reject("profile.invalid", exception.getMessage());
			}
		}
		model.addAttribute("profile", profileService.memberProfile(principal.getName()));
		addStoreContext(principal.getName(), store, model);
		return "member/profile";
	}

	private void addStoreContext(String email, String store, Model model) {
		if (hasStoreMembership(email, store)) {
			model.addAttribute("storeSlug", store);
		}
	}

	private boolean hasStoreMembership(String email, String store) {
		return store != null && !store.isBlank()
			&& membershipService.findMemberPoints(email, store).isPresent();
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

	@PostMapping("/s/{slug}/notification-preferences")
	@PreAuthorize("hasRole('MEMBER')")
	@ResponseBody
	public ResponseEntity<Void> updateStoreNotificationPreference(
		@PathVariable String slug,
		Principal principal,
		@RequestParam("preference") String preference,
		@RequestParam("enabled") boolean enabled
	) {
		try {
			membershipService.updateNotificationPreference(principal.getName(), slug, preference, enabled);
			return ResponseEntity.noContent().build();
		} catch (DomainRuleViolationException exception) {
			return ResponseEntity.badRequest().build();
		}
	}
}
