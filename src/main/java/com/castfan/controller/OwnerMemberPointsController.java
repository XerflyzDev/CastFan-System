package com.castfan.controller;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

import com.castfan.dto.OwnerMemberPointsForm;
import com.castfan.dto.OwnerPointAdjustmentForm;
import com.castfan.dto.OwnerMemberSearchResult;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.service.DomainConflictException;
import com.castfan.service.DomainRuleViolationException;
import com.castfan.service.MembershipService;
import jakarta.validation.Valid;
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

@Controller
@PreAuthorize("hasRole('OWNER')")
public class OwnerMemberPointsController {

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
		@RequestParam(required = false) Long memberId, Model model) {
		model.addAttribute("query", q == null ? "" : q);
		model.addAttribute("members", searchMembers(q));
		var form = new OwnerMemberPointsForm();
		form.setMemberProfileId(memberId);
		model.addAttribute("form", form);
		model.addAttribute("adjustmentForm", new OwnerPointAdjustmentForm());
		prepareSelection(principal, memberId, model);
		return "owner/member-points";
	}

	@PostMapping("/owner/member-points/preview")
	public String preview(Principal principal, @Valid @ModelAttribute("form") OwnerMemberPointsForm form,
		BindingResult result, Model model) {
		if (result.hasErrors()) return renderForm(principal, form, model);
		try {
			var preview = membershipService.previewEarnPoints(principal.getName(), form.getMemberProfileId(),
				form.getPaidAmount(), form.getReceiptReference());
			model.addAttribute("preview", preview);
			model.addAttribute("selectedMember", membershipService.memberPointsForOwner(principal.getName(),
				form.getMemberProfileId()));
		} catch (DomainConflictException | DomainRuleViolationException exception) {
			model.addAttribute("error", exception.getMessage());
		}
		return renderForm(principal, form, model);
	}

	@PostMapping("/owner/member-points/confirm")
	public String confirm(Principal principal, @Valid @ModelAttribute("form") OwnerMemberPointsForm form,
		BindingResult result, Model model, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) return renderForm(principal, form, model);
		try {
			var outcome = membershipService.recordPurchasePoints(principal.getName(), form.getMemberProfileId(),
				form.getPaidAmount(), form.getReceiptReference());
			redirectAttributes.addFlashAttribute("success", outcome);
			return "redirect:/owner/member-points?memberId=" + form.getMemberProfileId();
		} catch (DomainConflictException | DomainRuleViolationException exception) {
			model.addAttribute("error", exception.getMessage());
			return renderForm(principal, form, model);
		}
	}

	@PostMapping("/owner/member-points/adjustment/preview")
	public String previewAdjustment(Principal principal, @Valid @ModelAttribute("adjustmentForm") OwnerPointAdjustmentForm form,
		BindingResult result, Model model) {
		if (!result.hasErrors()) {
			try {
				model.addAttribute("adjustmentPreview", membershipService.previewCorrection(principal.getName(),
					form.getOriginalTransactionId(), form.getCorrectedPaidAmount()));
			} catch (DomainConflictException | DomainRuleViolationException exception) {
				model.addAttribute("error", exception.getMessage());
			}
		}
		prepareSelection(principal, form.getMemberProfileId(), model);
		model.addAttribute("adjustmentForm", form);
		return "owner/member-points";
	}

	@PostMapping("/owner/member-points/adjustment/confirm")
	public String confirmAdjustment(Principal principal, @Valid @ModelAttribute("adjustmentForm") OwnerPointAdjustmentForm form,
		BindingResult result, Model model, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			prepareSelection(principal, form.getMemberProfileId(), model);
			model.addAttribute("adjustmentForm", form);
			return "owner/member-points";
		}
		try {
			redirectAttributes.addFlashAttribute("success", membershipService.correctPurchasePoints(
				principal.getName(), form.getOriginalTransactionId(), form.getCorrectedPaidAmount(), form.getReason()));
			return "redirect:/owner/member-points?memberId=" + form.getMemberProfileId();
		} catch (DomainConflictException | DomainRuleViolationException exception) {
			model.addAttribute("error", exception.getMessage());
			prepareSelection(principal, form.getMemberProfileId(), model);
			model.addAttribute("adjustmentForm", form);
			return "owner/member-points";
		}
	}

	@PostMapping("/owner/member-points/reversal")
	public String reverse(Principal principal, @RequestParam Long memberId, @RequestParam Long transactionId,
		@RequestParam String reason, RedirectAttributes redirectAttributes) {
		try {
			redirectAttributes.addFlashAttribute("success", membershipService.reversePurchasePoints(
				principal.getName(), transactionId, reason));
		} catch (DomainConflictException | DomainRuleViolationException exception) {
			redirectAttributes.addFlashAttribute("error", exception.getMessage());
		}
		return "redirect:/owner/member-points?memberId=" + memberId;
	}

	private String renderForm(Principal principal, OwnerMemberPointsForm form, Model model) {
		model.addAttribute("query", "");
		model.addAttribute("members", searchMembers(null));
		prepareSelection(principal, form.getMemberProfileId(), model);
		model.addAttribute("adjustmentForm", new OwnerPointAdjustmentForm());
		return "owner/member-points";
	}

	private void prepareSelection(Principal principal, Long memberId, Model model) {
		model.addAttribute("members", searchMembers(null));
		if (!model.containsAttribute("form")) {
			var purchaseForm = new OwnerMemberPointsForm();
			purchaseForm.setMemberProfileId(memberId);
			model.addAttribute("form", purchaseForm);
		}
		if (!model.containsAttribute("adjustmentForm")) {
			model.addAttribute("adjustmentForm", new OwnerPointAdjustmentForm());
		}
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

	private List<OwnerMemberSearchResult> searchMembers(String query) {
		var cleaned = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
		return readTransaction.execute(status -> memberProfileRepository.findAll().stream()
			.filter(profile -> profile.getUserAccount().getRole() == UserRole.MEMBER
				&& profile.getUserAccount().getStatus() == UserStatus.ACTIVE)
			.filter(profile -> cleaned.isEmpty()
				|| profile.getUserAccount().getDisplayName().toLowerCase(Locale.ROOT).contains(cleaned)
				|| profile.getUserAccount().getEmail().toLowerCase(Locale.ROOT).contains(cleaned))
			.limit(30)
			.map(profile -> new OwnerMemberSearchResult(profile.getId(), profile.getUserAccount().getDisplayName(),
				profile.getUserAccount().getEmail(), profile.getPointsBalance(),
				profile.getStoreMembershipTier().name().replace('_', ' ')))
			.toList());
	}
}
