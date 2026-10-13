package com.castfan.controller;

import com.castfan.dto.HomeViewModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("pageTitle", "CastFan");
		model.addAttribute("home", HomeViewModel.preview());
		return "index";
	}
}
