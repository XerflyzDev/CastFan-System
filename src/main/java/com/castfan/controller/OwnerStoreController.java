package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.OwnerCastAssignmentForm;
import com.castfan.dto.OwnerStoreForm;
import com.castfan.service.DomainConflictException;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.OwnerStoreService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('OWNER')")
public class OwnerStoreController {

	private final OwnerStoreService ownerStoreService;

	public OwnerStoreController(OwnerStoreService ownerStoreService) {
		this.ownerStoreService = ownerStoreService;
	}

	@GetMapping("/owner/stores")
	public String stores(Principal principal, Model model) {
		prepareForms(model);
		populate(principal, model, null);
		return "owner/stores";
	}

	@GetMapping("/owner/stores/{storeId}/edit")
	public String edit(Principal principal, @PathVariable Long storeId, Model model) {
		try {
			model.addAttribute("storeForm", ownerStoreService.formForEdit(principal.getName(), storeId));
			if (!model.containsAttribute("assignmentForm")) model.addAttribute("assignmentForm", new OwnerCastAssignmentForm());
			populate(principal, model, storeId);
			return "owner/stores";
		} catch (DomainRuleViolationException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found");
		}
	}

	@PostMapping("/owner/stores")
	public String create(Principal principal, @Valid @ModelAttribute("storeForm") OwnerStoreForm form,
		BindingResult bindingResult, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				ownerStoreService.create(principal.getName(), form);
				return "redirect:/owner/stores?created";
			} catch (DomainConflictException | DomainRuleViolationException exception) {
				bindingResult.reject("store.invalid", exception.getMessage());
			}
		}
		if (!model.containsAttribute("assignmentForm")) model.addAttribute("assignmentForm", new OwnerCastAssignmentForm());
		populate(principal, model, null);
		return "owner/stores";
	}

	@PostMapping("/owner/stores/{storeId}")
	public String update(Principal principal, @PathVariable Long storeId,
		@Valid @ModelAttribute("storeForm") OwnerStoreForm form, BindingResult bindingResult, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				ownerStoreService.update(principal.getName(), storeId, form);
				return "redirect:/owner/stores?saved";
			} catch (DomainRuleViolationException exception) {
				bindingResult.reject("store.invalid", exception.getMessage());
			}
		}
		if (!model.containsAttribute("assignmentForm")) model.addAttribute("assignmentForm", new OwnerCastAssignmentForm());
		populate(principal, model, storeId);
		return "owner/stores";
	}

	@PostMapping("/owner/stores/{storeId}/{action:activate|deactivate}")
	public String storeStatus(Principal principal, @PathVariable Long storeId, @PathVariable String action,
		RedirectAttributes redirectAttributes) {
		return run(() -> ownerStoreService.setStoreActive(principal.getName(), storeId, action.equals("activate")),
			action.equals("activate") ? "activated" : "deactivated", redirectAttributes);
	}

	@PostMapping("/owner/stores/cast")
	public String assignCast(Principal principal,
		@Valid @ModelAttribute("assignmentForm") OwnerCastAssignmentForm form, BindingResult bindingResult, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				ownerStoreService.assignCast(principal.getName(), form);
				return "redirect:/owner/stores?assigned";
			} catch (DomainConflictException | DomainRuleViolationException exception) {
				bindingResult.reject("assignment.invalid", exception.getMessage());
			}
		}
		if (!model.containsAttribute("storeForm")) model.addAttribute("storeForm", defaultStoreForm());
		populate(principal, model, null);
		return "owner/stores";
	}

	@PostMapping("/owner/stores/cast/{assignmentId}/{action:activate|deactivate}")
	public String assignmentStatus(Principal principal, @PathVariable Long assignmentId, @PathVariable String action,
		RedirectAttributes redirectAttributes) {
		return run(() -> ownerStoreService.setAssignmentActive(principal.getName(), assignmentId, action.equals("activate")),
			action.equals("activate") ? "restored" : "removed", redirectAttributes);
	}

	private String run(Runnable action, String success, RedirectAttributes redirectAttributes) {
		try {
			action.run();
			redirectAttributes.addAttribute(success, true);
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addAttribute("error", exception.getMessage());
		}
		return "redirect:/owner/stores";
	}

	private void prepareForms(Model model) {
		if (!model.containsAttribute("storeForm")) model.addAttribute("storeForm", defaultStoreForm());
		if (!model.containsAttribute("assignmentForm")) model.addAttribute("assignmentForm", new OwnerCastAssignmentForm());
	}

	private OwnerStoreForm defaultStoreForm() {
		return new OwnerStoreForm();
	}

	private void populate(Principal principal, Model model, Long editingStoreId) {
		var storeView = ownerStoreService.load(principal.getName(), editingStoreId);
		model.addAttribute("storeView", storeView);
		model.addAttribute("storeFormAction", editingStoreId == null ? "/owner/stores" : "/owner/stores/" + editingStoreId);
		if (model.getAttribute("assignmentForm") instanceof OwnerCastAssignmentForm form
			&& form.getStoreId() == null && !storeView.stores().isEmpty()) {
			form.setStoreId(storeView.stores().getFirst().id());
		}
	}
}
