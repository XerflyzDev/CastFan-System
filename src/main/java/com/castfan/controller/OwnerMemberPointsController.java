package com.castfan.controller;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

import com.castfan.dto.OwnerMemberDirectoryPage;
import com.castfan.dto.OwnerMemberDirectoryState;
import com.castfan.dto.OwnerMemberPointsForm;
import com.castfan.dto.OwnerMemberSearchResult;
import com.castfan.dto.OwnerPointAdjustmentForm;
import com.castfan.entity.MemberProfile;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.service.DomainConflictException;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@PreAuthorize("hasRole('OWNER')")
public class OwnerMemberPointsController {

	private static final int PAGE_SIZE = 25;
	private static final int MAX_SAFE_PAGE = Integer.MAX_VALUE / PAGE_SIZE;
	private final MembershipService membershipService;
	private final MemberProfileRepository memberProfileRepository;
	private final TransactionTemplate readTransaction;

	public OwnerMemberPointsController(MembershipService membershipService,
		MemberProfileRepository memberProfileRepository, PlatformTransactionManager transactionManager) {
		this.membershipService = membershipService;
		this.memberProfileRepository = memberProfileRepository;
		this.readTransaction = new TransactionTemplate(transactionManager);
		this.readTransaction.setReadOnly(true);
	}

	@GetMapping("/owner/member-points")
	public String page(Principal principal, @RequestParam(required = false) String q,
		@RequestParam(required = false) Long memberId, @RequestParam(required = false) String page,
		@RequestParam(required = false) Long anchor, Model model) {
		var directory = loadDirectory(principal.getName(), q, page);
		var state = new OwnerMemberDirectoryState(cleanQuery(q), directory.currentPage(), anchor != null ? anchor : memberId);
		addDirectory(model, directory, state);
		var form = new OwnerMemberPointsForm();
		form.setMemberProfileId(memberId);
		model.addAttribute("form", form);
		model.addAttribute("adjustmentForm", new OwnerPointAdjustmentForm());
		prepareSelection(principal, memberId, model);
		return "owner/member-points";
	}

	@PostMapping("/owner/member-points/preview")
	public String preview(Principal principal, @Valid @ModelAttribute("form") OwnerMemberPointsForm form,
		BindingResult result, Model model, @RequestParam(required = false) String q,
		@RequestParam(required = false) String page, @RequestParam(required = false) Long anchor) {
		var state = resolveState(principal.getName(), q, page, anchor, form.getMemberProfileId());
		if (result.hasErrors()) return renderForm(principal, form, state, model);
		try {
			var preview = membershipService.previewEarnPoints(principal.getName(), form.getMemberProfileId(),
				form.getPaidAmount(), form.getReceiptReference());
			model.addAttribute("preview", preview);
			model.addAttribute("selectedMember", membershipService.memberPointsForOwner(principal.getName(),
				form.getMemberProfileId()));
		} catch (DomainConflictException | DomainRuleViolationException exception) {
			model.addAttribute("error", exception.getMessage());
		}
		return renderForm(principal, form, state, model);
	}

	@PostMapping("/owner/member-points/confirm")
	public String confirm(Principal principal, @Valid @ModelAttribute("form") OwnerMemberPointsForm form,
		BindingResult result, Model model, RedirectAttributes redirectAttributes,
		@RequestParam(required = false) String q, @RequestParam(required = false) String page,
		@RequestParam(required = false) Long anchor) {
		var state = resolveState(principal.getName(), q, page, anchor, form.getMemberProfileId());
		if (result.hasErrors()) return renderForm(principal, form, state, model);
		try {
			var outcome = membershipService.recordPurchasePoints(principal.getName(), form.getMemberProfileId(),
				form.getPaidAmount(), form.getReceiptReference());
			redirectAttributes.addFlashAttribute("success", outcome);
			return detailRedirect(form.getMemberProfileId(), state);
		} catch (DomainConflictException | DomainRuleViolationException exception) {
			model.addAttribute("error", exception.getMessage());
			return renderForm(principal, form, state, model);
		}
	}

	@PostMapping("/owner/member-points/adjustment/preview")
	public String previewAdjustment(Principal principal, @Valid @ModelAttribute("adjustmentForm") OwnerPointAdjustmentForm form,
		BindingResult result, Model model, @RequestParam(required = false) String q,
		@RequestParam(required = false) String page, @RequestParam(required = false) Long anchor) {
		var state = resolveState(principal.getName(), q, page, anchor, form.getMemberProfileId());
		if (!result.hasErrors()) {
			try {
				model.addAttribute("adjustmentPreview", membershipService.previewCorrection(principal.getName(),
					form.getOriginalTransactionId(), form.getCorrectedPaidAmount()));
			} catch (DomainConflictException | DomainRuleViolationException exception) {
				model.addAttribute("error", exception.getMessage());
			}
		}
		addDirectory(model, loadDirectory(principal.getName(), state.query(), String.valueOf(state.page())), state);
		prepareSelection(principal, form.getMemberProfileId(), model);
		model.addAttribute("adjustmentForm", form);
		return "owner/member-points";
	}

	@PostMapping("/owner/member-points/adjustment/confirm")
	public String confirmAdjustment(Principal principal, @Valid @ModelAttribute("adjustmentForm") OwnerPointAdjustmentForm form,
		BindingResult result, Model model, RedirectAttributes redirectAttributes,
		@RequestParam(required = false) String q, @RequestParam(required = false) String page,
		@RequestParam(required = false) Long anchor) {
		var state = resolveState(principal.getName(), q, page, anchor, form.getMemberProfileId());
		if (result.hasErrors()) {
			addDirectory(model, loadDirectory(principal.getName(), state.query(), String.valueOf(state.page())), state);
			prepareSelection(principal, form.getMemberProfileId(), model);
			model.addAttribute("adjustmentForm", form);
			return "owner/member-points";
		}
		try {
			redirectAttributes.addFlashAttribute("success", membershipService.correctPurchasePoints(
				principal.getName(), form.getOriginalTransactionId(), form.getCorrectedPaidAmount(), form.getReason()));
			return detailRedirect(form.getMemberProfileId(), state);
		} catch (DomainConflictException | DomainRuleViolationException exception) {
			model.addAttribute("error", exception.getMessage());
			addDirectory(model, loadDirectory(principal.getName(), state.query(), String.valueOf(state.page())), state);
			prepareSelection(principal, form.getMemberProfileId(), model);
			model.addAttribute("adjustmentForm", form);
			return "owner/member-points";
		}
	}

	@PostMapping("/owner/member-points/reversal")
	public String reverse(Principal principal, @RequestParam Long memberId, @RequestParam Long transactionId,
		@RequestParam String reason, @RequestParam(required = false) String q,
		@RequestParam(required = false) String page, @RequestParam(required = false) Long anchor,
		RedirectAttributes redirectAttributes) {
		var state = resolveState(principal.getName(), q, page, anchor, memberId);
		try {
			redirectAttributes.addFlashAttribute("success", membershipService.reversePurchasePoints(
				principal.getName(), transactionId, reason));
		} catch (DomainConflictException | DomainRuleViolationException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return detailRedirect(memberId, state);
	}

	private String renderForm(Principal principal, OwnerMemberPointsForm form, OwnerMemberDirectoryState state, Model model) {
		addDirectory(model, loadDirectory(principal.getName(), state.query(), String.valueOf(state.page())), state);
		prepareSelection(principal, form.getMemberProfileId(), model);
		model.addAttribute("form", form);
		if (!model.containsAttribute("adjustmentForm")) model.addAttribute("adjustmentForm", new OwnerPointAdjustmentForm());
		return "owner/member-points";
	}

	private void prepareSelection(Principal principal, Long memberId, Model model) {
		if (!model.containsAttribute("form")) {
			var purchaseForm = new OwnerMemberPointsForm();
			purchaseForm.setMemberProfileId(memberId);
			model.addAttribute("form", purchaseForm);
		}
		if (!model.containsAttribute("adjustmentForm")) model.addAttribute("adjustmentForm", new OwnerPointAdjustmentForm());
		if (memberId == null) return;
		try {
			var summary = membershipService.memberPointsForOwner(principal.getName(), memberId);
			model.addAttribute("selectedMember", summary);
			model.addAttribute("adjustedTransactionIds", summary.transactions().stream()
				.filter(transaction -> transaction.originalTransactionId() != null)
				.map(transaction -> transaction.originalTransactionId()).toList());
		} catch (DomainRuleViolationException exception) {
			model.addAttribute("error", "That active Member could not be found.");
		}
	}

	private OwnerMemberDirectoryPage loadDirectory(String ownerIdentifier, String query, String requestedPage) {
		var cleaned = cleanQuery(query);
		var requestedIndex = parsePage(requestedPage);
		return readTransaction.execute(status -> {
			var matching = membershipService.membershipsForOwner(ownerIdentifier).stream()
				.filter(membership -> {
					var account = membership.getMemberProfile().getUserAccount();
					var needle = cleaned.toLowerCase(Locale.ROOT);
					return needle.isBlank() || account.getDisplayName().toLowerCase(Locale.ROOT).contains(needle)
						|| (account.getEmail() != null && account.getEmail().toLowerCase(Locale.ROOT).contains(needle));
				})
				.sorted(java.util.Comparator.comparing(
					membership -> membership.getMemberProfile().getUserAccount().getDisplayName(),
					String.CASE_INSENSITIVE_ORDER))
				.toList();
			var totalPages = (int) Math.ceil(matching.size() / (double) PAGE_SIZE);
			var pageIndex = Math.min(requestedIndex, Math.max(0, totalPages - 1));
			var start = Math.min(pageIndex * PAGE_SIZE, matching.size());
			var end = Math.min(start + PAGE_SIZE, matching.size());
			var members = matching.subList(start, end).stream().map(OwnerMemberPointsController::toSearchResult).toList();
			return new OwnerMemberDirectoryPage(members, matching.size(), pageIndex, totalPages, PAGE_SIZE);
		});
	}

	private static OwnerMemberSearchResult toSearchResult(com.castfan.entity.StoreMembership membership) {
		var profile = membership.getMemberProfile();
		var account = profile.getUserAccount();
		return new OwnerMemberSearchResult(profile.getId(), account.getDisplayName(), account.getEmail(),
			membership.getPointsBalance(), membership.getTier().name().replace('_', ' '));
	}

	private void addDirectory(Model model, OwnerMemberDirectoryPage directory, OwnerMemberDirectoryState state) {
		model.addAttribute("directory", directory);
		model.addAttribute("members", directory.members());
		model.addAttribute("query", state.query());
		model.addAttribute("directoryState", state);
		model.addAttribute("directoryReturnUrl", directoryUrl(state));
	}

	private OwnerMemberDirectoryState resolveState(String ownerIdentifier, String query, String page, Long anchor, Long memberId) {
		var directory = loadDirectory(ownerIdentifier, query, page);
		return new OwnerMemberDirectoryState(cleanQuery(query), directory.currentPage(), anchor != null ? anchor : memberId);
	}

	private String detailRedirect(Long memberId, OwnerMemberDirectoryState state) {
		var builder = UriComponentsBuilder.fromPath("/owner/member-points").queryParam("memberId", memberId);
		if (!state.query().isBlank()) builder.queryParam("q", state.query());
		builder.queryParam("page", state.page());
		if (state.anchor() != null) builder.queryParam("anchor", state.anchor());
		if (state.anchor() != null) builder.fragment("member-" + state.anchor());
		return "redirect:" + builder.build().encode().toUriString();
	}

	private String directoryUrl(OwnerMemberDirectoryState state) {
		var builder = UriComponentsBuilder.fromPath("/owner/member-points");
		if (!state.query().isBlank()) builder.queryParam("q", state.query());
		builder.queryParam("page", state.page());
		if (state.anchor() != null) builder.queryParam("anchor", state.anchor()).fragment("member-" + state.anchor());
		return builder.build().encode().toUriString();
	}

	private static String cleanQuery(String query) {
		return query == null ? "" : query.trim();
	}

	private static int parsePage(String page) {
		if (page == null || page.isBlank()) return 0;
		try {
			return (int) Math.min(MAX_SAFE_PAGE, Math.max(0L, Long.parseLong(page)));
		} catch (NumberFormatException exception) {
			return 0;
		}
	}
}
