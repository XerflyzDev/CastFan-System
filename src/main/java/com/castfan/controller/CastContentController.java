package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.CastContentForm;
import com.castfan.service.CastContentService;
import com.castfan.service.DomainRuleViolationException;
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
@PreAuthorize("hasRole('CAST')")
public class CastContentController {
	private final CastContentService castContentService;

	public CastContentController(CastContentService castContentService) { this.castContentService = castContentService; }

	@GetMapping("/cast/content")
	public String content(Principal principal, Model model) {
		if (!model.containsAttribute("form")) model.addAttribute("form", new CastContentForm());
		populate(principal, model, null);
		return "cast/content";
	}

	@GetMapping("/cast/content/{contentId}/edit")
	public String edit(Principal principal, @PathVariable Long contentId, Model model) {
		try {
			model.addAttribute("form", castContentService.formForEdit(principal.getName(), contentId));
			populate(principal, model, contentId);
			return "cast/content";
		} catch (DomainRuleViolationException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Content not found");
		}
	}

	@PostMapping("/cast/content")
	public String create(Principal principal, @Valid @ModelAttribute("form") CastContentForm form,
		BindingResult bindingResult, Model model) {
		return save(principal, null, form, bindingResult, model);
	}

	@PostMapping("/cast/content/{contentId}")
	public String update(Principal principal, @PathVariable Long contentId,
		@Valid @ModelAttribute("form") CastContentForm form, BindingResult bindingResult, Model model) {
		return save(principal, contentId, form, bindingResult, model);
	}

	@PostMapping("/cast/content/{contentId}/publish")
	public String publish(Principal principal, @PathVariable Long contentId, RedirectAttributes attributes) {
		return act(() -> castContentService.publish(principal.getName(), contentId), "published", attributes);
	}

	@PostMapping("/cast/content/{contentId}/archive")
	public String archive(Principal principal, @PathVariable Long contentId, RedirectAttributes attributes) {
		return act(() -> castContentService.archive(principal.getName(), contentId), "archived", attributes);
	}

	private String save(Principal principal, Long contentId, CastContentForm form,
		BindingResult bindingResult, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				if (contentId == null) castContentService.create(principal.getName(), form);
				else castContentService.update(principal.getName(), contentId, form);
				return "redirect:/cast/content?" + (contentId == null ? "created" : "saved");
			} catch (DomainRuleViolationException exception) {
				bindingResult.reject("content.invalid", exception.getMessage());
			}
		}
		populate(principal, model, contentId);
		return "cast/content";
	}

	private String act(Runnable action, String success, RedirectAttributes attributes) {
		try { action.run(); attributes.addAttribute(success, true); }
		catch (DomainRuleViolationException exception) { attributes.addAttribute("error", exception.getMessage()); }
		return "redirect:/cast/content";
	}

	private void populate(Principal principal, Model model, Long editingContentId) {
		model.addAttribute("contentView", castContentService.load(principal.getName(), editingContentId));
		model.addAttribute("contentFormAction", editingContentId == null ? "/cast/content" : "/cast/content/" + editingContentId);
	}
}
