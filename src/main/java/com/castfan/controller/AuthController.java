package com.castfan.controller;

import java.nio.charset.StandardCharsets;

import com.castfan.dto.RegistrationForm;
import com.castfan.service.DuplicateEmailException;
import com.castfan.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

	private final RegistrationService registrationService;

	public AuthController(RegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@GetMapping("/login")
	public String login() {
		return "auth/login";
	}

	@GetMapping("/signup")
	public String signup(Model model) {
		if (!model.containsAttribute("form")) {
			model.addAttribute("form", new RegistrationForm());
		}
		return "auth/signup";
	}

	@PostMapping("/signup")
	public String register(
		@Valid @ModelAttribute("form") RegistrationForm form,
		BindingResult bindingResult
	) {
		if (!form.getPassword().equals(form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
		}
		if (form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
			bindingResult.rejectValue("password", "password.tooLong", "Password must not exceed 72 bytes");
		}

		if (bindingResult.hasErrors()) {
			return "auth/signup";
		}

		try {
			registrationService.registerMember(form);
		} catch (DuplicateEmailException exception) {
			bindingResult.rejectValue("email", "email.duplicate", exception.getMessage());
			return "auth/signup";
		}

		return "redirect:/login?registered";
	}
}
