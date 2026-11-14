package com.castfan.controller;

import java.security.Principal;

import com.castfan.service.HomeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
	private final HomeService homeService;

	public HomeController(HomeService homeService) {
		this.homeService = homeService;
	}

	@GetMapping("/")
	public String home(Principal principal, Model model) {
		if (principal == null) {
			return "welcome";
		}
		model.addAttribute("pageTitle", "CastFan");
		model.addAttribute("home", homeService.load(principal.getName()));
		return "index";
	}
}
