package com.castfan.controller;

import java.security.Principal;

import com.castfan.dto.AccountView;
import com.castfan.entity.UserRole;
import com.castfan.repository.UserAccountRepository;
import com.castfan.service.AccountService;
import com.castfan.service.AccountAvatarService;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.MembershipService;
import com.castfan.service.ProfileService;
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
		var account = userAccountRepository.findByLoginIdentifierIgnoreCase(principal.getName())
			.orElseThrow(() -> new UsernameNotFoundException("Account not found"));
		if (account.getRole() == UserRole.CAST) {
			return "redirect:/cast/profile";
		}
		model.addAttribute("account", AccountView.from(account, accountAvatarService.hasAvatar(account.getEmail())));
		model.addAttribute(
			"memberPoints",
			account.getRole() == UserRole.MEMBER
				? membershipService.findMemberPoints(account.getEmail()).orElse(null)
				: null
		);
		model.addAttribute(
			"memberProfile",
			account.getRole() == UserRole.MEMBER ? profileService.memberProfile(account.getEmail()) : null
		);
		return "account/index";
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
	public String uploadAvatar(@RequestParam("avatar") MultipartFile avatar, Principal principal,
		RedirectAttributes redirectAttributes) {
		try {
			accountAvatarService.upload(principal.getName(), avatar);
			return "redirect:/account?photoSaved";
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addFlashAttribute("photoError", exception.getMessage());
			return "redirect:/account";
		}
	}

	@PostMapping("/account/avatar/remove")
	public String removeAvatar(Principal principal) {
		accountAvatarService.remove(principal.getName());
		return "redirect:/account?photoRemoved";
	}

	@PostMapping("/account/name")
	public String updateDisplayName(@RequestParam("displayName") String displayName, Principal principal,
		RedirectAttributes redirectAttributes) {
		try {
			accountService.updateDisplayName(principal.getName(), displayName);
			return "redirect:/account?nameSaved";
		} catch (DomainRuleViolationException exception) {
			redirectAttributes.addFlashAttribute("nameError", exception.getMessage());
			redirectAttributes.addFlashAttribute("attemptedName", displayName);
			return "redirect:/account";
		}
	}
}
