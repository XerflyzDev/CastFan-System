package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.OwnerCastAssignmentForm;
import com.castfan.dto.OwnerCastPromotionForm;
import com.castfan.dto.OwnerCastCreateForm;
import com.castfan.service.DomainConflictException;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.OwnerStoreService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('OWNER')")
public class OwnerStoreController {
	private final OwnerStoreService ownerStoreService;

	public OwnerStoreController(OwnerStoreService ownerStoreService) { this.ownerStoreService = ownerStoreService; }

	@GetMapping({"/owner/cast", "/owner/stores"})
	public String roster(Principal principal, Model model) {
		populate(principal, model);
		return "owner/stores";
	}

	@PostMapping("/owner/cast/create")
	public String createCast(Principal principal, @Valid @ModelAttribute("createForm") OwnerCastCreateForm form,
		BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
		if (!bindingResult.hasErrors()) {
			try {
				var credentials = ownerStoreService.createCast(principal.getName(), form);
				redirectAttributes.addFlashAttribute("newCredentials", credentials);
				return "redirect:/owner/cast?created";
			} catch (DomainConflictException | DomainRuleViolationException exception) {
				bindingResult.reject("cast.invalid", exception.getMessage());
			}
		}
		populate(principal, model);
		return "owner/stores";
	}

	@PostMapping("/owner/cast/{assignmentId}/reset-password")
	public String resetPassword(Principal principal, @PathVariable Long assignmentId,
		RedirectAttributes redirectAttributes) {
		try {
			redirectAttributes.addFlashAttribute("newCredentials",
				ownerStoreService.resetCastPassword(principal.getName(), assignmentId));
			redirectAttributes.addAttribute("reset", true);
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addAttribute("error", exception.getMessage());
		}
		return "redirect:/owner/cast";
	}

	@PostMapping("/owner/cast/{assignmentId}/{action:activate|deactivate}")
	public String assignmentStatus(Principal principal, @PathVariable Long assignmentId, @PathVariable String action,
		RedirectAttributes redirectAttributes) {
		try {
			ownerStoreService.setAssignmentActive(principal.getName(), assignmentId, action.equals("activate"));
			redirectAttributes.addAttribute(action.equals("activate") ? "restored" : "removed", true);
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addAttribute("error", exception.getMessage());
		}
		return "redirect:/owner/cast";
	}

	private void populate(Principal principal, Model model) {
		if (!model.containsAttribute("assignmentForm")) {
			model.addAttribute("assignmentForm", new OwnerCastAssignmentForm());
		}
		if (!model.containsAttribute("promotionForm")) {
			model.addAttribute("promotionForm", new OwnerCastPromotionForm());
		}
		if (!model.containsAttribute("createForm")) model.addAttribute("createForm", new OwnerCastCreateForm());
		model.addAttribute("storeView", ownerStoreService.load(principal.getName()));
	}
}
