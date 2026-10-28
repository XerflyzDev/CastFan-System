package com.castfan.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.castfan.dto.OwnerEventForm;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.OwnerEventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('OWNER')")
public class OwnerEventController {
	private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Bangkok");

	private final OwnerEventService ownerEventService;
	private final com.castfan.service.StoreEventImageService storeEventImageService;

	public OwnerEventController(OwnerEventService ownerEventService,
		com.castfan.service.StoreEventImageService storeEventImageService) {
		this.ownerEventService = ownerEventService;
		this.storeEventImageService = storeEventImageService;
	}

	@GetMapping("/owner/events")
	public String events(Principal principal, Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", defaultForm());
		}
		populate(principal, model, null);
		return "owner/events";
	}

	@GetMapping("/owner/events/{eventId}/edit")
	public String edit(Principal principal, @PathVariable Long eventId, Model model) {
		try {
			model.addAttribute("form", ownerEventService.formForEdit(principal.getName(), eventId));
			populate(principal, model, eventId);
			return "owner/events";
		} catch (DomainRuleViolationException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
		}
	}

	@PostMapping("/owner/events")
	public String create(
		Principal principal,
		@Valid @ModelAttribute("form") OwnerEventForm form,
		BindingResult bindingResult,
		@RequestParam(name = "image", required = false) MultipartFile image,
		Model model
	) {
		if (!bindingResult.hasErrors()) {
			try {
				ownerEventService.create(principal.getName(), form, image);
				return "redirect:/owner/events?created";
			} catch (DomainRuleViolationException exception) {
				bindingResult.reject("event.invalid", exception.getMessage());
			}
		}
		populate(principal, model, null);
		return "owner/events";
	}

	@PostMapping("/owner/events/{eventId}")
	public String update(
		Principal principal,
		@PathVariable Long eventId,
		@Valid @ModelAttribute("form") OwnerEventForm form,
		BindingResult bindingResult,
		@RequestParam(name = "image", required = false) MultipartFile image,
		Model model
	) {
		if (!bindingResult.hasErrors()) {
			try {
				ownerEventService.update(principal.getName(), eventId, form, image);
				return "redirect:/owner/events?saved";
			} catch (DomainRuleViolationException exception) {
				bindingResult.reject("event.invalid", exception.getMessage());
			}
		}
		populate(principal, model, eventId);
		return "owner/events";
	}

	@GetMapping("/owner/events/{eventId}/image")
	public ResponseEntity<byte[]> image(Principal principal, @PathVariable Long eventId) {
		return storeEventImageService.loadOwned(principal.getName(), eventId)
			.map(image -> ResponseEntity.ok()
				.cacheControl(CacheControl.noCache())
				.contentType(MediaType.parseMediaType(image.contentType()))
				.body(image.imageData()))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/owner/events/{eventId}/publish")
	public String publish(
		Principal principal,
		@PathVariable Long eventId,
		RedirectAttributes redirectAttributes
	) {
		return changeStatus(() -> ownerEventService.publish(principal.getName(), eventId), "published", redirectAttributes);
	}

	@PostMapping("/owner/events/{eventId}/archive")
	public String archive(
		Principal principal,
		@PathVariable Long eventId,
		RedirectAttributes redirectAttributes
	) {
		return changeStatus(() -> ownerEventService.archive(principal.getName(), eventId), "archived", redirectAttributes);
	}

	private String changeStatus(Runnable action, String successParameter, RedirectAttributes redirectAttributes) {
		try {
			action.run();
			redirectAttributes.addAttribute(successParameter, true);
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addAttribute("error", exception.getMessage());
		}
		return "redirect:/owner/events";
	}

	private void populate(Principal principal, Model model, Long editingEventId) {
		model.addAttribute("eventView", ownerEventService.load(principal.getName(), editingEventId));
		model.addAttribute("eventFormAction", editingEventId == null
			? "/owner/events"
			: "/owner/events/" + editingEventId);
	}

	private OwnerEventForm defaultForm() {
		var form = new OwnerEventForm();
		form.setDate(LocalDate.now(STORE_ZONE).plusDays(7));
		form.setStartsAt(LocalTime.of(16, 0));
		form.setEndsAt(LocalTime.of(21, 0));
		return form;
	}
}
