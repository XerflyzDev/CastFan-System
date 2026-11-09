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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.castfan.service.StorePromotionImageService;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
@PreAuthorize("hasRole('OWNER')")
public class OwnerPromotionController {
	private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Bangkok");
	private final OwnerPromotionService ownerPromotionService;
	private final StorePromotionImageService storePromotionImageService;

	public OwnerPromotionController(OwnerPromotionService ownerPromotionService,
		StorePromotionImageService storePromotionImageService) {
		this.ownerPromotionService = ownerPromotionService;
		this.storePromotionImageService = storePromotionImageService;
	}

	@GetMapping("/owner/promotions")
	public String promotions(Principal principal, Model model) {
		model.addAttribute("promotionView", ownerPromotionService.load(principal.getName(), null));
		return "owner/promotions";
	}

	@GetMapping("/owner/promotions/new")
	public String newPromotion(Principal principal, Model model) {
		if (!model.containsAttribute("form")) model.addAttribute("form", defaultForm());
		populate(principal, model, null);
		return "owner/promotion-form";
	}

	@GetMapping("/owner/promotions/{promotionId}/edit")
	public String edit(Principal principal, @PathVariable Long promotionId, Model model) {
		try {
			model.addAttribute("form", ownerPromotionService.formForEdit(principal.getName(), promotionId));
			populate(principal, model, promotionId);
			return "owner/promotion-form";
		} catch (DomainRuleViolationException exception) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Promotion not found");
		}
	}

	@PostMapping("/owner/promotions")
	public String create(Principal principal, @Valid @ModelAttribute("form") OwnerPromotionForm form,
		BindingResult bindingResult, @RequestParam(name = "image", required = false) MultipartFile image, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				ownerPromotionService.create(principal.getName(), form, image);
				return "redirect:/owner/promotions?created";
			} catch (DomainConflictException | DomainRuleViolationException exception) {
				bindingResult.reject("promotion.invalid", exception.getMessage());
			}
		}
		populate(principal, model, null);
		return "owner/promotion-form";
	}

	@PostMapping("/owner/promotions/{promotionId}")
	public String update(Principal principal, @PathVariable Long promotionId,
		@Valid @ModelAttribute("form") OwnerPromotionForm form, BindingResult bindingResult,
		@RequestParam(name = "image", required = false) MultipartFile image, Model model) {
		if (!bindingResult.hasErrors()) {
			try {
				ownerPromotionService.update(principal.getName(), promotionId, form, image);
				return "redirect:/owner/promotions?saved";
			} catch (DomainConflictException | DomainRuleViolationException exception) {
				bindingResult.reject("promotion.invalid", exception.getMessage());
			}
		}
		populate(principal, model, promotionId);
		return "owner/promotion-form";
	}

	@GetMapping("/owner/promotions/{promotionId}/image")
	public ResponseEntity<byte[]> image(Principal principal, @PathVariable Long promotionId) {
		return storePromotionImageService.loadOwned(principal.getName(), promotionId)
			.map(image -> ResponseEntity.ok().cacheControl(CacheControl.noCache())
				.contentType(MediaType.parseMediaType(image.contentType())).body(image.imageData()))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/owner/promotions/{promotionId}/publish")
	public String publish(Principal principal, @PathVariable Long promotionId,
		RedirectAttributes redirectAttributes) {
		try {
			ownerPromotionService.publish(principal.getName(), promotionId);
			redirectAttributes.addAttribute("published", true);
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addAttribute("error", exception.getMessage());
		}
		return "redirect:/owner/promotions";
	}

	@PostMapping("/owner/promotions/{promotionId}/archive")
	public String archive(Principal principal, @PathVariable Long promotionId,
		RedirectAttributes redirectAttributes) {
		try {
			ownerPromotionService.archive(principal.getName(), promotionId);
			redirectAttributes.addAttribute("archived", true);
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addAttribute("error", exception.getMessage());
		}
		return "redirect:/owner/promotions";
	}

	private void populate(Principal principal, Model model, Long editingPromotionId) {
		model.addAttribute("promotionView", ownerPromotionService.load(principal.getName(), editingPromotionId));
		model.addAttribute("promotionFormAction", editingPromotionId == null
			? "/owner/promotions" : "/owner/promotions/" + editingPromotionId);
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
