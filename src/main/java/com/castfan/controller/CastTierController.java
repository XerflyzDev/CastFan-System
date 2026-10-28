package com.castfan.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@PreAuthorize("hasRole('CAST')")
public class CastTierController {

	@GetMapping("/cast/tiers")
	public String tiers() {
		return "redirect:/cast/content?legacyTiers";
	}
}
