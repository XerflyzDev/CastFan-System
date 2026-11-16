package com.castfan.config;

import com.castfan.repository.CastProfileRepository;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the Cast's own Store slug available to the shared Cast navigation so the
 * Schedule tab opens that Store's approved Member schedule.
 */
@ControllerAdvice
public class CastNavigationAdvice {

	private final CastProfileRepository castProfileRepository;

	public CastNavigationAdvice(CastProfileRepository castProfileRepository) {
		this.castProfileRepository = castProfileRepository;
	}

	@ModelAttribute
	public void addCastStoreSlug(Authentication authentication, Model model) {
		if (authentication == null || model.containsAttribute("castStoreSlug")
			|| authentication.getAuthorities().stream()
				.noneMatch(authority -> "ROLE_CAST".equals(authority.getAuthority()))) {
			return;
		}
		castProfileRepository.findByUserAccountLoginIdentifierIgnoreCase(authentication.getName())
			.map(profile -> profile.getStore())
			.filter(store -> store != null && store.isActive())
			.ifPresent(store -> model.addAttribute("castStoreSlug", store.getSlug()));
	}
}
