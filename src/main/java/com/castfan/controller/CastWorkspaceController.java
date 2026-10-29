package com.castfan.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.castfan.dto.CastProfileForm;
import com.castfan.dto.CastShiftForm;
import com.castfan.service.CastWorkspaceService;
import com.castfan.service.DomainConflictException;
import com.castfan.service.DomainRuleViolationException;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('CAST')")
public class CastWorkspaceController {

	private final CastWorkspaceService castWorkspaceService;

	public CastWorkspaceController(CastWorkspaceService castWorkspaceService) {
		this.castWorkspaceService = castWorkspaceService;
	}

	@GetMapping("/cast")
	public String dashboard(Principal principal, Model model) {
		model.addAttribute("workspace", castWorkspaceService.load(principal.getName(), 6));
		return "cast/dashboard";
	}

	@GetMapping("/cast/schedule")
	public String schedule(Principal principal, Model model) {
		if (!model.containsAttribute("form")) {
			var form = new CastShiftForm();
			form.setDate(LocalDate.now().plusDays(1));
			form.setStartsAt(LocalTime.of(14, 0));
			form.setEndsAt(LocalTime.of(20, 0));
			model.addAttribute("form", form);
		}
		populateSchedule(principal, model);
		return "cast/schedule";
	}

	@PostMapping("/cast/schedule")
	public String submitShift(
		Principal principal,
		@Valid @ModelAttribute("form") CastShiftForm form,
		BindingResult bindingResult,
		Model model
	) {
		if (!bindingResult.hasErrors()) {
			try {
				castWorkspaceService.submitShift(principal.getName(), form);
				return "redirect:/cast/schedule?submitted";
			} catch (DomainRuleViolationException exception) {
				bindingResult.reject("shift.invalid", exception.getMessage());
			}
		}
		populateSchedule(principal, model);
		return "cast/schedule";
	}

	@PostMapping("/cast/shifts/{shiftId}/cancel")
	public String cancelShift(
		Principal principal,
		@PathVariable Long shiftId,
		RedirectAttributes redirectAttributes
	) {
		try {
			castWorkspaceService.cancelShift(principal.getName(), shiftId);
			redirectAttributes.addAttribute("cancelled", true);
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addAttribute("error", exception.getMessage());
		}
		return "redirect:/cast/schedule";
	}

	@GetMapping("/cast/profile")
	public String profile(Principal principal, Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", castWorkspaceService.profileForm(principal.getName()));
		}
		model.addAttribute("profileEditorOpen", false);
		model.addAttribute("nameEditorOpen", false);
		model.addAttribute("workspace", castWorkspaceService.load(principal.getName(), 3));
		return "cast/profile";
	}

	@PostMapping("/cast/profile")
	public String updateProfile(
		Principal principal,
		@Valid @ModelAttribute("form") CastProfileForm form,
		BindingResult bindingResult,
		@RequestParam(name = "image", required = false) MultipartFile image,
		Model model
	) {
		if (!bindingResult.hasErrors()) {
			try {
				castWorkspaceService.updateProfile(principal.getName(), form, image);
				return "redirect:/cast/profile?saved";
			} catch (DomainConflictException | DomainRuleViolationException exception) {
				bindingResult.reject("profile.invalid", exception.getMessage());
			}
		}
		model.addAttribute("profileEditorOpen", true);
		model.addAttribute("nameEditorOpen", bindingResult.hasFieldErrors("stageName"));
		model.addAttribute("workspace", castWorkspaceService.load(principal.getName(), 3));
		return "cast/profile";
	}

	private void populateSchedule(Principal principal, Model model) {
		model.addAttribute("workspace", castWorkspaceService.load(principal.getName(), 50));
	}
}
