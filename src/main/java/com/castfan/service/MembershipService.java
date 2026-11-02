package com.castfan.service;

import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;

import com.castfan.dto.MemberPointTransactionView;
import com.castfan.dto.MembershipPointsPreview;
import com.castfan.dto.MembershipPointsResult;
import com.castfan.dto.MembershipPointsSummary;
import com.castfan.entity.MemberPointTransaction;
import com.castfan.entity.MemberPointTransactionType;
import com.castfan.entity.MemberProfile;
import com.castfan.entity.StoreMembershipTier;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.MemberPointTransactionRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembershipService {

	private final UserAccountRepository userAccountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final MemberPointTransactionRepository memberPointTransactionRepository;

	public MembershipService(
		UserAccountRepository userAccountRepository,
		MemberProfileRepository memberProfileRepository,
		MemberPointTransactionRepository memberPointTransactionRepository
	) {
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.memberPointTransactionRepository = memberPointTransactionRepository;
	}

	@Transactional(readOnly = true)
	public MembershipPointsPreview previewEarnPoints(
		String authenticatedOwnerEmail,
		Long memberProfileId,
		java.math.BigDecimal paidAmount,
		String receiptReference
	) {
		requireActiveAccount(authenticatedOwnerEmail, UserRole.OWNER);
		var member = requirePointsMember(memberProfileId);
		var normalizedAmount = normalizePaidAmount(paidAmount);
		var receipt = normalizeReceipt(receiptReference);
		if (memberPointTransactionRepository.existsByReceiptReferenceKey(receipt.key())) {
			throw new DomainConflictException("This receipt reference has already been used.");
		}
		return preview(member, pointsFor(normalizedAmount));
	}

	@Transactional
	public MembershipPointsResult recordPurchasePoints(
		String authenticatedOwnerEmail,
		Long memberProfileId,
		java.math.BigDecimal paidAmount,
		String receiptReference
	) {
		var owner = requireActiveAccount(authenticatedOwnerEmail, UserRole.OWNER);
		var normalizedAmount = normalizePaidAmount(paidAmount);
		var receipt = normalizeReceipt(receiptReference);
		if (memberPointTransactionRepository.existsByReceiptReferenceKey(receipt.key())) {
			throw new DomainConflictException("This receipt reference has already been used.");
		}
		var member = memberProfileRepository.findByIdForUpdate(memberProfileId)
			.filter(profile -> profile.getUserAccount().getRole() == UserRole.MEMBER
				&& profile.getUserAccount().getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Active Member profile not found."));
		var points = pointsFor(normalizedAmount);
		applyPoints(member, points);
		var entry = new MemberPointTransaction(
			member,
			owner,
			MemberPointTransactionType.EARN,
			normalizedAmount,
			points,
			receipt.display(),
			receipt.key(),
			null,
			null
		);
		return savePointChange(member, entry, "This receipt reference has already been used.");
	}

	@Transactional(readOnly = true)
	public MembershipPointsPreview previewCorrection(
		String authenticatedOwnerEmail,
		Long originalTransactionId,
		java.math.BigDecimal correctedPaidAmount
	) {
		requireActiveAccount(authenticatedOwnerEmail, UserRole.OWNER);
		var original = requireEarnTransaction(originalTransactionId);
		if (memberPointTransactionRepository.existsByOriginalTransactionId(original.getId())) {
			throw new DomainConflictException("This point entry has already been adjusted.");
		}
		var correctedPoints = pointsFor(normalizePaidAmount(correctedPaidAmount));
		return preview(original.getMemberProfile(), correctedPoints - original.getPointsDelta());
	}

	@Transactional
	public MembershipPointsResult correctPurchasePoints(
		String authenticatedOwnerEmail,
		Long originalTransactionId,
		java.math.BigDecimal correctedPaidAmount,
		String reason
	) {
		var owner = requireActiveAccount(authenticatedOwnerEmail, UserRole.OWNER);
		var adjustmentReason = requireReason(reason);
		var original = requireEarnTransactionForUpdate(originalTransactionId);
		ensureNotAdjusted(original);
		var normalizedAmount = normalizePaidAmount(correctedPaidAmount);
		var correctedPoints = pointsFor(normalizedAmount);
		var delta = correctedPoints - original.getPointsDelta();
		var member = lockPointsMember(original.getMemberProfile().getId());
		applyPoints(member, delta);
		var entry = new MemberPointTransaction(
			member,
			owner,
			MemberPointTransactionType.CORRECTION,
			normalizedAmount,
			delta,
			null,
			null,
			adjustmentReason,
			original
		);
		return savePointChange(member, entry, "This point entry has already been adjusted.");
	}

	@Transactional
	public MembershipPointsResult reversePurchasePoints(
		String authenticatedOwnerEmail,
		Long originalTransactionId,
		String reason
	) {
		var owner = requireActiveAccount(authenticatedOwnerEmail, UserRole.OWNER);
		var adjustmentReason = requireReason(reason);
		var original = requireEarnTransactionForUpdate(originalTransactionId);
		ensureNotAdjusted(original);
		var member = lockPointsMember(original.getMemberProfile().getId());
		var delta = -original.getPointsDelta();
		applyPoints(member, delta);
		var entry = new MemberPointTransaction(
			member,
			owner,
			MemberPointTransactionType.REVERSAL,
			null,
			delta,
			null,
			null,
			adjustmentReason,
			original
		);
		return savePointChange(member, entry, "This point entry has already been adjusted.");
	}

	@Transactional(readOnly = true)
	public MembershipPointsSummary memberPoints(String authenticatedMemberEmail) {
		return pointsSummary(requireActiveMember(authenticatedMemberEmail));
	}

	@Transactional(readOnly = true)
	public MembershipPointsSummary memberPointsForOwner(String authenticatedOwnerEmail, Long memberProfileId) {
		requireActiveAccount(authenticatedOwnerEmail, UserRole.OWNER);
		return pointsSummary(requirePointsMember(memberProfileId));
	}

	@Transactional(readOnly = true)
	public boolean hasStoreTierAccess(String authenticatedMemberEmail, int minimumRank) {
		if (minimumRank < 1 || minimumRank > 3) {
			throw new DomainRuleViolationException("Minimum store tier rank must be between 1 and 3.");
		}
		return requireActiveMember(authenticatedMemberEmail).getStoreMembershipTier().getRank() >= minimumRank;
	}

	private MemberProfile requirePointsMember(Long memberProfileId) {
		if (memberProfileId == null) {
			throw new DomainRuleViolationException("Member profile is required.");
		}
		return memberProfileRepository.findById(memberProfileId)
			.filter(profile -> profile.getUserAccount().getRole() == UserRole.MEMBER
				&& profile.getUserAccount().getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Active Member profile not found."));
	}

	private MemberProfile lockPointsMember(Long memberProfileId) {
		return memberProfileRepository.findByIdForUpdate(memberProfileId)
			.filter(profile -> profile.getUserAccount().getRole() == UserRole.MEMBER
				&& profile.getUserAccount().getStatus() == UserStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Active Member profile not found."));
	}

	private MemberPointTransaction requireEarnTransaction(Long transactionId) {
		if (transactionId == null) {
			throw new DomainRuleViolationException("Original point transaction is required.");
		}
		var transaction = memberPointTransactionRepository.findById(transactionId)
			.orElseThrow(() -> new DomainRuleViolationException("Point transaction not found."));
		if (transaction.getType() != MemberPointTransactionType.EARN) {
			throw new DomainRuleViolationException("Only an earned-points entry can be adjusted.");
		}
		return transaction;
	}

	private MemberPointTransaction requireEarnTransactionForUpdate(Long transactionId) {
		if (transactionId == null) {
			throw new DomainRuleViolationException("Original point transaction is required.");
		}
		var transaction = memberPointTransactionRepository.findByIdForUpdate(transactionId)
			.orElseThrow(() -> new DomainRuleViolationException("Point transaction not found."));
		if (transaction.getType() != MemberPointTransactionType.EARN) {
			throw new DomainRuleViolationException("Only an earned-points entry can be adjusted.");
		}
		return transaction;
	}

	private void ensureNotAdjusted(MemberPointTransaction original) {
		if (memberPointTransactionRepository.existsByOriginalTransactionId(original.getId())) {
			throw new DomainConflictException("This point entry has already been adjusted.");
		}
	}

	private MembershipPointsPreview preview(MemberProfile member, long delta) {
		long resultingBalance;
		try {
			resultingBalance = Math.addExact(member.getPointsBalance(), delta);
		} catch (ArithmeticException exception) {
			throw new DomainRuleViolationException("Point balance is outside the supported range.");
		}
		if (resultingBalance < 0) {
			throw new DomainRuleViolationException("Point balance cannot be negative.");
		}
		return new MembershipPointsPreview(
			member.getId(),
			member.getUserAccount().getDisplayName(),
			member.getPointsBalance(),
			delta,
			resultingBalance,
			member.getStoreMembershipTier(),
			StoreMembershipTier.fromPoints(resultingBalance)
		);
	}

	private MembershipPointsResult savePointChange(
		MemberProfile member,
		MemberPointTransaction entry,
		String conflictMessage
	) {
		try {
			memberProfileRepository.save(member);
			var saved = memberPointTransactionRepository.saveAndFlush(entry);
			return new MembershipPointsResult(
				MemberPointTransactionView.from(saved),
				member.getPointsBalance(),
				member.getStoreMembershipTier()
			);
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException(conflictMessage);
		}
	}

	private MembershipPointsSummary pointsSummary(MemberProfile member) {
		var history = memberPointTransactionRepository
			.findAllByMemberProfileIdOrderByCreatedAtDescIdDesc(member.getId()).stream()
			.map(MemberPointTransactionView::from)
			.toList();
		return new MembershipPointsSummary(
			member.getId(),
			member.getUserAccount().getDisplayName(),
			member.getPointsBalance(),
			member.getStoreMembershipTier(),
			history
		);
	}

	private void applyPoints(MemberProfile member, long delta) {
		try {
			member.applyPointsDelta(delta);
		} catch (IllegalArgumentException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
	}

	private java.math.BigDecimal normalizePaidAmount(java.math.BigDecimal amount) {
		if (amount == null || amount.signum() <= 0) {
			throw new DomainRuleViolationException("Paid amount must be positive.");
		}
		if (amount.stripTrailingZeros().scale() > 2
			|| amount.precision() - amount.scale() > 10) {
			throw new DomainRuleViolationException("Paid amount must have up to 10 digits and 2 decimal places.");
		}
		return amount.setScale(2, RoundingMode.UNNECESSARY);
	}

	private long pointsFor(java.math.BigDecimal amount) {
		try {
			return amount.setScale(0, RoundingMode.FLOOR).longValueExact();
		} catch (ArithmeticException exception) {
			throw new DomainRuleViolationException("Paid amount is outside the supported range.");
		}
	}

	private ReceiptReference normalizeReceipt(String value) {
		var display = value == null ? "" : Normalizer.normalize(value.trim(), Normalizer.Form.NFKC);
		if (display.isEmpty() || display.length() > 120) {
			throw new DomainRuleViolationException("Receipt reference is required and must not exceed 120 characters.");
		}
		return new ReceiptReference(display, display.toLowerCase(Locale.ROOT));
	}

	private String requireReason(String reason) {
		var cleaned = reason == null ? "" : reason.trim();
		if (cleaned.isEmpty() || cleaned.length() > 500) {
			throw new DomainRuleViolationException("Adjustment reason is required and must not exceed 500 characters.");
		}
		return cleaned;
	}

	private record ReceiptReference(String display, String key) {
	}

	private MemberProfile requireActiveMember(String email) {
		var account = requireActiveAccount(email, UserRole.MEMBER);
		return memberProfileRepository.findByUserAccountId(account.getId())
			.orElseThrow(() -> new DomainRuleViolationException("Member profile not found."));
	}

	private UserAccount requireActiveAccount(String email, UserRole role) {
		var account = userAccountRepository.findByEmailIgnoreCase(email)
			.orElseThrow(() -> new DomainRuleViolationException("Account not found."));
		if (account.getRole() != role || account.getStatus() != UserStatus.ACTIVE) {
			throw new DomainRuleViolationException("An active " + role.name() + " account is required.");
		}
		return account;
	}

}
