package com.castfan.controller;

import java.nio.charset.StandardCharsets;

import com.castfan.dto.RegistrationForm;
import com.castfan.dto.StoreSignupForm;
import com.castfan.service.DuplicateEmailException;
import com.castfan.service.RegistrationService;
import com.castfan.service.StoreEntryService;
import com.castfan.service.StoreSignupService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

	private final RegistrationService registrationService;
	private final Environment environment;
	private final StoreSignupService storeSignupService;
	private final StoreEntryService storeEntryService;

	public AuthController(RegistrationService registrationService, Environment environment,
		StoreSignupService storeSignupService, StoreEntryService storeEntryService) {
		this.registrationService = registrationService;
		this.environment = environment;
		this.storeSignupService = storeSignupService;
		this.storeEntryService = storeEntryService;
	}

	@ModelAttribute
	public void addAuthPreviewFlags(Model model) {
		model.addAttribute("showGoogleLoginMock", environment.acceptsProfiles(Profiles.of("dev")));
	}

	@GetMapping("/login")
	public String login(@RequestParam(required = false) String store, HttpSession session, Model model) {
		if (store != null && storeEntryService.isActiveSlug(store)) {
			session.setAttribute(StoreEntryService.SESSION_STORE_SLUG, store);
			model.addAttribute("storeContext", store);
		}
		return "auth/login";
	}

	@GetMapping("/signup/store")
	public String storeSignup(@RequestParam(required = false) String name, Model model) {
		if (!model.containsAttribute("form")) {
			var form = new StoreSignupForm();
			if (name != null && !name.isBlank()) {
				form.setStoreName(name.trim());
				form.setSlug(storeSignupService.suggestSlug(name));
			}
			model.addAttribute("form", form);
		}
		return "auth/store-signup";
	}

	@PostMapping("/signup/store")
	public String registerStore(@Valid @ModelAttribute("form") StoreSignupForm form, BindingResult bindingResult) {
		if (!form.getPassword().equals(form.getConfirmPassword())) {
			bindingResult.rejectValue("confirmPassword", "password.mismatch", "Passwords do not match");
		}
		if (form.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
			bindingResult.rejectValue("password", "password.tooLong", "Password must not exceed 72 bytes");
		}
		if (bindingResult.hasErrors()) return "auth/store-signup";
		try {
			storeSignupService.register(form);
		} catch (IllegalArgumentException exception) {
			bindingResult.reject("store.duplicate", exception.getMessage());
			return "auth/store-signup";
		}
		return "redirect:/login?storeRegistered";
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
