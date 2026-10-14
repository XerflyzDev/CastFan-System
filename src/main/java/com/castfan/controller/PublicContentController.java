package com.castfan.controller;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

import com.castfan.service.PublicContentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicContentController {

	private final PublicContentService publicContentService;

	public PublicContentController(PublicContentService publicContentService) {
		this.publicContentService = publicContentService;
	}

	@GetMapping("/casts/{castProfileId}")
	public String castProfile(@PathVariable Long castProfileId, Principal principal, Model model) {
		model.addAttribute(
			"cast",
			publicContentService.castProfile(castProfileId, principal == null ? null : principal.getName())
		);
		return "public/cast-profile";
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
}
