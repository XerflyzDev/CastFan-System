package com.castfan.controller;

import java.security.Principal;
import java.time.LocalDate;

import com.castfan.service.DomainConflictException;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.OwnerWorkspaceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('OWNER')")
public class OwnerWorkspaceController {

	private final OwnerWorkspaceService ownerWorkspaceService;

	public OwnerWorkspaceController(OwnerWorkspaceService ownerWorkspaceService) {
		this.ownerWorkspaceService = ownerWorkspaceService;
	}

	@GetMapping("/owner")
	public String dashboard(Principal principal, Model model) {
		model.addAttribute("workspace", ownerWorkspaceService.load(principal.getName(), null));
		return "owner/dashboard";
	}

	@GetMapping("/owner/shifts")
	public String shifts(
		Principal principal,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		Model model
	) {
		model.addAttribute("workspace", ownerWorkspaceService.load(principal.getName(), date));
		return "owner/shifts";
	}

	@PostMapping("/owner/shifts/{shiftId}/approve")
	public String approve(
		Principal principal,
		@PathVariable Long shiftId,
		@RequestParam(required = false) String reviewNote,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		RedirectAttributes redirectAttributes
	) {
		try {
			ownerWorkspaceService.approve(principal.getName(), shiftId, reviewNote);
			redirectAttributes.addAttribute("approved", true);
		} catch (DomainConflictException | DomainRuleViolationException exception) {
			redirectAttributes.addAttribute("error", exception.getMessage());
		}
		addDate(redirectAttributes, date);
		return "redirect:/owner/shifts";
	}

	@PostMapping("/owner/shifts/{shiftId}/reject")
	public String reject(
		Principal principal,
		@PathVariable Long shiftId,
		@RequestParam(required = false) String reviewNote,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
		RedirectAttributes redirectAttributes
	) {
		try {
			ownerWorkspaceService.reject(principal.getName(), shiftId, reviewNote);
			redirectAttributes.addAttribute("rejected", true);
		} catch (DomainConflictException | DomainRuleViolationException exception) {
			redirectAttributes.addAttribute("error", exception.getMessage());
		}
		addDate(redirectAttributes, date);
		return "redirect:/owner/shifts";
	}

	private void addDate(RedirectAttributes redirectAttributes, LocalDate date) {
		if (date != null) {
			redirectAttributes.addAttribute("date", date.toString());
		}
	}
}
