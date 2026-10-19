package com.castfan.controller;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

import com.castfan.service.PublicContentService;
import com.castfan.service.StoreEventImageService;
import com.castfan.service.StorePromotionImageService;
import com.castfan.service.CastContentImageService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicContentController {

	private final PublicContentService publicContentService;
	private final StoreEventImageService storeEventImageService;
	private final StorePromotionImageService storePromotionImageService;
	private final CastContentImageService castContentImageService;

	public PublicContentController(PublicContentService publicContentService,
		StoreEventImageService storeEventImageService, StorePromotionImageService storePromotionImageService,
		CastContentImageService castContentImageService) {
		this.publicContentService = publicContentService;
		this.storeEventImageService = storeEventImageService;
		this.storePromotionImageService = storePromotionImageService;
		this.castContentImageService = castContentImageService;
	}

	@GetMapping("/casts/{castProfileId}")
	public String castProfile(@PathVariable Long castProfileId, Principal principal, Model model) {
		model.addAttribute("ownProfile", false);
		model.addAttribute(
			"cast",
			publicContentService.castProfile(castProfileId, principal == null ? null : principal.getName())
		);
		return "public/cast-profile";
	}

	@GetMapping("/cast/public-profile")
	@PreAuthorize("hasRole('CAST')")
	public String ownCastProfile(Principal principal, Model model) {
		model.addAttribute("ownProfile", true);
		model.addAttribute("cast", publicContentService.ownCastProfile(principal.getName()));
		return "public/cast-profile";
	}

	@GetMapping("/casts/{castProfileId}/content/{contentId}")
	public String content(@PathVariable Long castProfileId, @PathVariable Long contentId,
		Principal principal, Model model) {
		model.addAttribute("content", publicContentService.content(
			castProfileId, contentId, principal == null ? null : principal.getName()
		));
		return "public/content-detail";
	}

	@GetMapping("/events/{eventId}")
	public String event(@PathVariable Long eventId, Model model) {
		model.addAttribute("event", publicContentService.event(eventId));
		return "public/event-detail";
	}

	@GetMapping("/events/{eventId}/calendar.ics")
	public ResponseEntity<byte[]> eventCalendar(@PathVariable Long eventId) {
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=castfan-event-" + eventId + ".ics")
			.body(publicContentService.eventCalendar(eventId).getBytes(StandardCharsets.UTF_8));
	}

	@GetMapping("/events/{eventId}/image")
	public ResponseEntity<byte[]> eventImage(@PathVariable Long eventId) {
		return storeEventImageService.loadPublic(eventId)
			.map(image -> ResponseEntity.ok()
				.cacheControl(CacheControl.noCache())
				.contentType(MediaType.parseMediaType(image.contentType()))
				.body(image.imageData()))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/promotions/{promotionId}/image")
	public ResponseEntity<byte[]> promotionImage(@PathVariable Long promotionId) {
		return storePromotionImageService.loadPublic(promotionId)
			.map(image -> ResponseEntity.ok().cacheControl(CacheControl.noCache())
				.contentType(MediaType.parseMediaType(image.contentType())).body(image.imageData()))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/casts/{castProfileId}/content/{contentId}/image")
	public ResponseEntity<byte[]> contentImage(@PathVariable Long castProfileId, @PathVariable Long contentId,
		Principal principal) {
		var content = publicContentService.content(castProfileId, contentId, principal == null ? null : principal.getName());
		if (!content.accessible()) throw new org.springframework.web.server.ResponseStatusException(
			org.springframework.http.HttpStatus.NOT_FOUND, "Content image not found");
		return castContentImageService.loadPublic(castProfileId, contentId)
			.map(image -> ResponseEntity.ok().cacheControl(CacheControl.noCache())
				.contentType(MediaType.parseMediaType(image.contentType())).body(image.imageData()))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}
}
