package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.AccountView;
import com.castfan.dto.MembershipPointsSummary;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.repository.UserAccountRepository;
import com.castfan.service.AccountService;
import com.castfan.service.AccountAvatarService;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.MembershipService;
import com.castfan.service.ProfileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

	private final UserAccountRepository userAccountRepository;
	private final MembershipService membershipService;
	private final ProfileService profileService;
	private final AccountAvatarService accountAvatarService;
	private final AccountService accountService;

	public AccountController(UserAccountRepository userAccountRepository, MembershipService membershipService,
		ProfileService profileService, AccountAvatarService accountAvatarService, AccountService accountService) {
		this.userAccountRepository = userAccountRepository;
		this.membershipService = membershipService;
		this.profileService = profileService;
		this.accountAvatarService = accountAvatarService;
		this.accountService = accountService;
	}

	@GetMapping("/account")
	public String account(Principal principal, Model model) {
		var account = requireAccount(principal);
		if (account.getRole() == UserRole.CAST) {
			return "redirect:/cast/profile";
		}
		var memberPoints = account.getRole() == UserRole.MEMBER
			? membershipService.findMemberPoints(account.getEmail()).orElse(null)
			: null;
		return renderAccount(account, null, memberPoints, model);
	}

	@GetMapping("/s/{slug}/account")
	@PreAuthorize("hasRole('MEMBER')")
	public String storeAccount(@PathVariable String slug, Principal principal, Model model) {
		var account = requireAccount(principal);
		var memberPoints = membershipService.findMemberPoints(account.getEmail(), slug).orElse(null);
		if (memberPoints == null) {
			return "redirect:/s/" + slug;
		}
		return renderAccount(account, slug, memberPoints, model);
	}

	@GetMapping("/account/avatar")
	public ResponseEntity<byte[]> avatar(Principal principal) {
		return accountAvatarService.load(principal.getName())
			.map(avatar -> ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.contentType(MediaType.parseMediaType(avatar.contentType()))
				.body(avatar.imageData()))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@GetMapping("/avatars/{accountId}")
	public ResponseEntity<byte[]> publicAvatar(@PathVariable Long accountId) {
		return accountAvatarService.loadPublic(accountId)
			.map(avatar -> ResponseEntity.ok()
				.cacheControl(CacheControl.noCache())
				.contentType(MediaType.parseMediaType(avatar.contentType()))
				.body(avatar.imageData()))
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@PostMapping("/account/avatar")
	public String uploadAvatar(@RequestParam("avatar") MultipartFile avatar,
		@RequestParam(name = "store", required = false) String store, Principal principal,
		RedirectAttributes redirectAttributes) {
		try {
			accountAvatarService.upload(principal.getName(), avatar);
			return accountRedirect(principal.getName(), store, "?photoSaved");
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addFlashAttribute("photoError", exception.getMessage());
			return accountRedirect(principal.getName(), store, "");
		}
	}

	@PostMapping("/account/avatar/remove")
	public String removeAvatar(@RequestParam(name = "store", required = false) String store, Principal principal) {
		accountAvatarService.remove(principal.getName());
		return accountRedirect(principal.getName(), store, "?photoRemoved");
	}

	@PostMapping("/account/name")
	public String updateDisplayName(@RequestParam("displayName") String displayName,
		@RequestParam(name = "store", required = false) String store, Principal principal,
		RedirectAttributes redirectAttributes) {
		try {
			accountService.updateDisplayName(principal.getName(), displayName);
			return accountRedirect(principal.getName(), store, "?nameSaved");
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addFlashAttribute("nameError", exception.getMessage());
			redirectAttributes.addFlashAttribute("attemptedName", displayName);
			return accountRedirect(principal.getName(), store, "");
		}
	}

	private String renderAccount(UserAccount account, String storeSlug, MembershipPointsSummary memberPoints, Model model) {
		model.addAttribute("account", AccountView.from(account, accountAvatarService.hasAvatar(account.getEmail())));
		model.addAttribute("memberPoints", memberPoints);
		model.addAttribute("memberProfile",
			account.getRole() == UserRole.MEMBER ? profileService.memberProfile(account.getEmail()) : null);
		model.addAttribute("storeSlug", storeSlug);
		return "account/index";
	}

	private String accountRedirect(String email, String store, String query) {
		if (store != null && !store.isBlank()) {
			try {
				if (membershipService.findMemberPoints(email, store).isPresent()) {
					return "redirect:/s/" + store + "/account" + query;
				}
			} catch (DomainRuleViolationException ignored) {
				// Fall back to the global account page for non-Member or invalid store context.
			}
		}
		return "redirect:/account" + query;
	}

	private UserAccount requireAccount(Principal principal) {
		return userAccountRepository.findByLoginIdentifierIgnoreCase(principal.getName())
			.orElseThrow(() -> new UsernameNotFoundException("Account not found"));
	}
}
