package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.StoreSettingsForm;
import com.castfan.service.OwnerSettingsService;
import com.castfan.service.QrCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasRole('OWNER')")
public class OwnerSettingsController {
	private final OwnerSettingsService settings;
	private final QrCodeService qrCodes;
	public OwnerSettingsController(OwnerSettingsService settings, QrCodeService qrCodes) { this.settings = settings; this.qrCodes = qrCodes; }

	@GetMapping("/owner/store/settings")
	public String settings(Principal principal, Model model) {
		var store = settings.requireStore(principal.getName());
		if (!model.containsAttribute("form")) model.addAttribute("form", settings.form(principal.getName()));
		model.addAttribute("store", store);
		return "owner/store-settings";
	}

	@PostMapping("/owner/store/settings")
	public String update(Principal principal, @Valid @ModelAttribute("form") StoreSettingsForm form,
		BindingResult errors, Model model) {
		if (errors.hasErrors()) {
			model.addAttribute("store", settings.requireStore(principal.getName()));
			return "owner/store-settings";
		}
		settings.update(principal.getName(), form);
		return "redirect:/owner/store/settings?saved";
	}

	@GetMapping(value = "/owner/store/qr", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> qr(Principal principal, HttpServletRequest request,
		@RequestParam(defaultValue = "false") boolean download) {
		var store = settings.requireStore(principal.getName());
		var url = request.getScheme() + "://" + request.getServerName()
			+ ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort())
			+ request.getContextPath() + "/s/" + store.getSlug();
		var headers = new HttpHeaders();
		if (download) headers.setContentDisposition(ContentDisposition.attachment().filename(store.getSlug() + "-qr.png").build());
		return ResponseEntity.ok().headers(headers).contentType(MediaType.IMAGE_PNG).body(qrCodes.png(url, 640));
	}
}
