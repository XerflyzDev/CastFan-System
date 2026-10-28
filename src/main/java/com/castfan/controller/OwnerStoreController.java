package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.OwnerCastAssignmentForm;
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
		if (!model.containsAttribute("assignmentForm")) model.addAttribute("assignmentForm", new OwnerCastAssignmentForm());
		populate(principal, model);
		return "owner/stores";
	}

	@PostMapping("/owner/cast")
	public String assignCast(Principal principal, @Valid @ModelAttribute("assignmentForm") OwnerCastAssignmentForm form,
		BindingResult bindingResult, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				ownerStoreService.assignCast(principal.getName(), form);
				return "redirect:/owner/cast?assigned";
			} catch (DomainConflictException | DomainRuleViolationException exception) {
				bindingResult.reject("assignment.invalid", exception.getMessage());
			}
		}
		populate(principal, model);
		return "owner/stores";
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
		model.addAttribute("storeView", ownerStoreService.load(principal.getName()));
	}
}
