package com.castfan.controller;

import java.security.Principal;

import com.castfan.entity.UserRole;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.StoreMembershipRepository;
import com.castfan.repository.UserAccountRepository;
import com.castfan.service.StoreEntryService;
import com.castfan.service.StorefrontService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class StorefrontController {
	private final StorefrontService storefronts;
	private final StoreEntryService entries;
	private final UserAccountRepository accounts;
	private final MemberProfileRepository members;
	private final StoreMembershipRepository memberships;

	public StorefrontController(StorefrontService storefronts, StoreEntryService entries,
		UserAccountRepository accounts, MemberProfileRepository members, StoreMembershipRepository memberships) {
		this.storefronts = storefronts; this.entries = entries; this.accounts = accounts;
		this.members = members; this.memberships = memberships;
	}

	@GetMapping("/s/{slug}")
	public String storefront(@PathVariable String slug, Principal principal, Model model, HttpSession session) {
		var view = load(slug);
		model.addAttribute("storefront", view);
		model.addAttribute("storeSlug", slug);
		model.addAttribute("memberJoined", isJoined(principal, view.store().id()));
		if (principal == null) session.setAttribute(StoreEntryService.SESSION_STORE_SLUG, slug);
		return "store/front";
	}

	@GetMapping("/s/{slug}/home")
	public String memberHome(@PathVariable String slug, Principal principal, Model model) {
		var view = load(slug);
		if (!isJoined(principal, view.store().id())) return "redirect:/s/" + slug;
		model.addAttribute("storefront", view);
		model.addAttribute("storeSlug", slug);
		model.addAttribute("memberJoined", true);
		return "store/front";
	}

	@PostMapping("/s/{slug}/join")
	public String join(@PathVariable String slug, Principal principal) {
		if (principal == null) return "redirect:/login?store=" + slug;
		var account = accounts.findByLoginIdentifierIgnoreCase(principal.getName()).orElseThrow();
		return "redirect:" + entries.joinAndHome(account, slug);
	}

	@GetMapping("/account/stores")
	public String myStores(Principal principal, Model model) {
		var account = accounts.findByLoginIdentifierIgnoreCase(principal.getName()).orElseThrow();
		if (account.getRole() != UserRole.MEMBER) return "redirect:/";
		var member = members.findByUserAccountId(account.getId()).orElseThrow();
		model.addAttribute("memberships", memberships.findAllByMemberProfileOrderByJoinedAtAsc(member));
		return "member/my-stores";
	}

	private boolean isJoined(Principal principal, Long storeId) {
		if (principal == null) return false;
		return accounts.findByLoginIdentifierIgnoreCase(principal.getName())
			.filter(a -> a.getRole() == UserRole.MEMBER)
			.flatMap(a -> members.findByUserAccountId(a.getId()))
			.map(m -> memberships.findAllByMemberProfileOrderByJoinedAtAsc(m).stream()
				.anyMatch(ms -> ms.getStore().getId().equals(storeId)))
			.orElse(false);
	}

	private StorefrontService.StorefrontView load(String slug) {
		try { return storefronts.load(slug); }
		catch (StorefrontService.StoreNotFoundException exception) { throw new ResponseStatusException(HttpStatus.NOT_FOUND); }
	}
}
