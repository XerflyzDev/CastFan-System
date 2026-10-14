package com.castfan.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.castfan.dto.OwnerPromotionForm;
import com.castfan.service.DomainConflictException;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.OwnerPromotionService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
@PreAuthorize("hasRole('OWNER')")
public class OwnerPromotionController {
	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Bangkok");
	private final OwnerPromotionService ownerPromotionService;

	public OwnerPromotionController(OwnerPromotionService ownerPromotionService) {
		this.ownerPromotionService = ownerPromotionService;
	}

	@GetMapping("/owner/promotions")
	public String promotions(Principal principal, Model model) {
		if (!model.containsAttribute("form")) model.addAttribute("form", defaultForm());
		populate(principal, model, null);
		return "owner/promotions";
	}

	@GetMapping("/owner/promotions/{promotionId}/edit")
	public String edit(Principal principal, @PathVariable Long promotionId, Model model) {
		try {
			model.addAttribute("form", ownerPromotionService.formForEdit(principal.getName(), promotionId));
			populate(principal, model, promotionId);
			return "owner/promotions";
		} catch (DomainRuleViolationException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found");
		}
	}

	@PostMapping("/owner/promotions")
	public String create(Principal principal, @Valid @ModelAttribute("form") OwnerPromotionForm form,
		BindingResult bindingResult, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				ownerPromotionService.create(principal.getName(), form);
				return "redirect:/owner/promotions?created";
			} catch (DomainConflictException | DomainRuleViolationException exception) {
				bindingResult.reject("promotion.invalid", exception.getMessage());
			}
		}
		populate(principal, model, null);
		return "owner/promotions";
	}

	@PostMapping("/owner/promotions/{promotionId}")
	public String update(Principal principal, @PathVariable Long promotionId,
		@Valid @ModelAttribute("form") OwnerPromotionForm form, BindingResult bindingResult, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				ownerPromotionService.update(principal.getName(), promotionId, form);
				return "redirect:/owner/promotions?saved";
			} catch (DomainConflictException | DomainRuleViolationException exception) {
				bindingResult.reject("promotion.invalid", exception.getMessage());
			}
		}
		populate(principal, model, promotionId);
		return "owner/promotions";
	}

	@PostMapping("/owner/promotions/{promotionId}/{action:activate|deactivate}")
	public String changeStatus(Principal principal, @PathVariable Long promotionId, @PathVariable String action,
		RedirectAttributes redirectAttributes) {
		try {
			ownerPromotionService.setActive(principal.getName(), promotionId, action.equals("activate"));
			redirectAttributes.addAttribute(action.equals("activate") ? "activated" : "deactivated", true);
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addAttribute("error", exception.getMessage());
		}
		return "redirect:/owner/promotions";
	}

	private void populate(Principal principal, Model model, Long editingPromotionId) {
		var view = ownerPromotionService.load(principal.getName(), editingPromotionId);
		model.addAttribute("promotionView", view);
		model.addAttribute("promotionFormAction", editingPromotionId == null
			? "/owner/promotions" : "/owner/promotions/" + editingPromotionId);
		if (model.getAttribute("form") instanceof OwnerPromotionForm form
			&& form.getStoreId() == null && !view.stores().isEmpty()) form.setStoreId(view.stores().getFirst().id());
	}

	private OwnerPromotionForm defaultForm() {
		var form = new OwnerPromotionForm();
		var today = LocalDate.now(DEFAULT_ZONE);
		form.setStartsOn(today);
		form.setStartsAt(LocalTime.of(9, 0));
		form.setEndsOn(today.plusDays(30));
		form.setEndsAt(LocalTime.of(23, 59));
		return form;
	}
}
