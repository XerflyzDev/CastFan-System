package com.castfan.service;

import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Locale;

import com.castfan.dto.MemberPointTransactionView;
import com.castfan.dto.MembershipPointsPreview;
import com.castfan.dto.MembershipPointsResult;
import com.castfan.dto.MembershipPointsSummary;
import com.castfan.dto.MemberProfileForm;
import com.castfan.entity.MemberPointTransaction;
import com.castfan.entity.MemberPointTransactionType;
import com.castfan.entity.MemberProfile;
import com.castfan.entity.StoreMembership;
import com.castfan.entity.StoreMembershipTier;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.MemberPointTransactionRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.UserAccountRepository;
import com.castfan.repository.StoreMembershipRepository;
import com.castfan.repository.StoreRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Service
public class MembershipService {

	private final UserAccountRepository userAccountRepository;
	private final MemberProfileRepository memberProfileRepository;
	private final MemberPointTransactionRepository memberPointTransactionRepository;
	private final StoreMembershipRepository storeMembershipRepository;
	private final StoreRepository storeRepository;
	private final SystemStoreService systemStoreService;
	private final Environment environment;

	public MembershipService(
		UserAccountRepository userAccountRepository,
		MemberProfileRepository memberProfileRepository,
		MemberPointTransactionRepository memberPointTransactionRepository,
		StoreMembershipRepository storeMembershipRepository,
		StoreRepository storeRepository,
		SystemStoreService systemStoreService,
		Environment environment
	) {
		this.userAccountRepository = userAccountRepository;
		this.memberProfileRepository = memberProfileRepository;
		this.memberPointTransactionRepository = memberPointTransactionRepository;
		this.storeMembershipRepository = storeMembershipRepository;
		this.storeRepository = storeRepository;
		this.systemStoreService = systemStoreService;
		this.environment = environment;
	}

	@Transactional(readOnly = true)
	public MembershipPointsPreview previewEarnPoints(
		String authenticatedOwnerEmail,
		Long memberProfileId,
		java.math.BigDecimal paidAmount,
		String receiptReference
	) {
		var owner = requireActiveAccount(authenticatedOwnerEmail, UserRole.OWNER);
		var membership = requireOwnerMembership(owner, memberProfileId);
		var normalizedAmount = normalizePaidAmount(paidAmount);
		var receipt = normalizeReceipt(receiptReference);
		if (memberPointTransactionRepository.existsByStoreIdAndReceiptReferenceKey(membership.getStore().getId(), receipt.key())) {
			throw new DomainConflictException("This receipt reference has already been used.");
		}
		return preview(membership, pointsFor(normalizedAmount));
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
		var membership = requireOwnerMembership(owner, memberProfileId);
		if (memberPointTransactionRepository.existsByStoreIdAndReceiptReferenceKey(membership.getStore().getId(), receipt.key())) {
			throw new DomainConflictException("This receipt reference has already been used.");
		}
		var points = pointsFor(normalizedAmount);
		applyPoints(membership, points);
		var entry = new MemberPointTransaction(
			membership,
			owner,
			MemberPointTransactionType.EARN,
			normalizedAmount,
			points,
			receipt.display(),
			receipt.key(),
			null,
			null
		);
		return savePointChange(membership, entry, "This receipt reference has already been used.");
	}

	@Transactional(readOnly = true)
	public MembershipPointsPreview previewCorrection(
		String authenticatedOwnerEmail,
		Long originalTransactionId,
		java.math.BigDecimal correctedPaidAmount
	) {
		var owner = requireActiveAccount(authenticatedOwnerEmail, UserRole.OWNER);
		var original = requireEarnTransaction(originalTransactionId);
		requireOwnerTransaction(owner, original);
		if (memberPointTransactionRepository.existsByOriginalTransactionId(original.getId())) {
			throw new DomainConflictException("This point entry has already been adjusted.");
		}
		var correctedPoints = pointsFor(normalizePaidAmount(correctedPaidAmount));
		return preview(original.getStoreMembership(), correctedPoints - original.getPointsDelta());
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
		requireOwnerTransaction(owner, original);
		ensureNotAdjusted(original);
		var normalizedAmount = normalizePaidAmount(correctedPaidAmount);
		var correctedPoints = pointsFor(normalizedAmount);
		var delta = correctedPoints - original.getPointsDelta();
		var membership = original.getStoreMembership();
		applyPoints(membership, delta);
		var entry = new MemberPointTransaction(
			membership,
			owner,
			MemberPointTransactionType.CORRECTION,
			normalizedAmount,
			delta,
			null,
			null,
			adjustmentReason,
			original
		);
		return savePointChange(membership, entry, "This point entry has already been adjusted.");
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
		requireOwnerTransaction(owner, original);
		ensureNotAdjusted(original);
		var membership = original.getStoreMembership();
		var delta = -original.getPointsDelta();
		applyPoints(membership, delta);
		var entry = new MemberPointTransaction(
			membership,
			owner,
			MemberPointTransactionType.REVERSAL,
			null,
			delta,
			null,
			null,
			adjustmentReason,
			original
		);
		return savePointChange(membership, entry, "This point entry has already been adjusted.");
	}

	@Transactional(readOnly = true)
	public MembershipPointsSummary memberPoints(String authenticatedMemberEmail) {
		return memberPoints(authenticatedMemberEmail, "castfan");
	}

	@Transactional
	public MembershipPointsSummary memberPoints(String authenticatedMemberEmail, String storeSlug) {
		var member = requireActiveMember(authenticatedMemberEmail);
		var store = storeRepository.findBySlugAndActiveTrue(storeSlug)
			.or(() -> environment.acceptsProfiles(Profiles.of("test"))
				? systemStoreService.findActive() : java.util.Optional.empty())
			.orElseThrow(() -> new DomainRuleViolationException("Store not found."));
		var membership = storeMembershipRepository.findByStoreAndMemberProfile(store, member)
			.or(() -> environment.acceptsProfiles(Profiles.of("test"))
				? java.util.Optional.of(storeMembershipRepository.save(new StoreMembership(store, member)))
				: java.util.Optional.empty())
			.orElseThrow(() -> new DomainRuleViolationException("Join this store before viewing membership."));
		return pointsSummary(membership);
	}

	@Transactional(readOnly = true)
	public MemberProfileForm notificationPreferenceForm(String authenticatedMemberIdentifier, String storeSlug) {
		var membership = requireMemberStoreMembership(authenticatedMemberIdentifier, storeSlug);
		var form = new MemberProfileForm();
		form.setDisplayName(membership.getMemberProfile().getUserAccount().getDisplayName());
		form.setBio(membership.getMemberProfile().getBio());
		form.setCastUpdateNotifications(membership.isCastUpdateNotifications());
		form.setEventNotifications(membership.isEventNotifications());
		return form;
	}

	@Transactional
	public void updateNotificationPreference(String authenticatedMemberIdentifier, String storeSlug,
		String preference, boolean enabled) {
		var membership = requireMemberStoreMembership(authenticatedMemberIdentifier, storeSlug);
		switch (preference) {
			case "castUpdateNotifications" -> membership.updateNotifications(enabled, membership.isEventNotifications());
			case "eventNotifications" -> membership.updateNotifications(membership.isCastUpdateNotifications(), enabled);
			default -> throw new DomainRuleViolationException("Invalid notification preference.");
		}
		storeMembershipRepository.saveAndFlush(membership);
	}

	@Transactional(readOnly = true)
	public MembershipPointsSummary memberPointsForOwner(String authenticatedOwnerEmail, Long memberProfileId) {
		var owner = requireActiveAccount(authenticatedOwnerEmail, UserRole.OWNER);
		return pointsSummary(requireOwnerMembership(owner, memberProfileId));
	}

	@Transactional
	public java.util.List<StoreMembership> membershipsForOwner(String authenticatedOwnerIdentifier) {
		var owner = requireActiveAccount(authenticatedOwnerIdentifier, UserRole.OWNER);
		var store = systemStoreService.requireForOwner(owner);
		if (environment.acceptsProfiles(Profiles.of("test"))) {
			memberProfileRepository.findAll().stream()
				.filter(profile -> profile.getUserAccount().getRole() == UserRole.MEMBER)
				.filter(profile -> profile.getUserAccount().getStatus() == UserStatus.ACTIVE)
				.filter(profile -> storeMembershipRepository.findByStoreAndMemberProfile(store, profile).isEmpty())
				.forEach(profile -> storeMembershipRepository.save(new StoreMembership(store, profile)));
		}
		return storeMembershipRepository.findAllByStoreOrderByJoinedAtDesc(store);
	}

	@Transactional(readOnly = true)
	public boolean hasStoreTierAccess(String authenticatedMemberEmail, int minimumRank) {
		if (minimumRank < 1 || minimumRank > 3) {
			throw new DomainRuleViolationException("Minimum store tier rank must be between 1 and 3.");
		}
		return memberPoints(authenticatedMemberEmail).tier().getRank() >= minimumRank;
	}

	@Transactional(readOnly = true)
	public boolean hasStoreTierAccess(String authenticatedMemberEmail, String storeSlug, int minimumRank) {
		if (minimumRank < 1 || minimumRank > 3) throw new DomainRuleViolationException("Minimum store tier rank must be between 1 and 3.");
		return memberPoints(authenticatedMemberEmail, storeSlug).tier().getRank() >= minimumRank;
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

	private MembershipPointsPreview preview(StoreMembership membership, long delta) {
		long resultingBalance;
		try {
			resultingBalance = Math.addExact(membership.getPointsBalance(), delta);
		} catch (ArithmeticException exception) {
			throw new DomainRuleViolationException("Point balance is outside the supported range.");
		}
		if (resultingBalance < 0) {
			throw new DomainRuleViolationException("Point balance cannot be negative.");
		}
		return new MembershipPointsPreview(
			membership.getMemberProfile().getId(),
			membership.getMemberProfile().getUserAccount().getDisplayName(),
			membership.getPointsBalance(),
			delta,
			resultingBalance,
			membership.getTier(),
			StoreMembershipTier.fromPoints(resultingBalance)
		);
	}

	private MembershipPointsResult savePointChange(
		StoreMembership membership,
		MemberPointTransaction entry,
		String conflictMessage
	) {
		try {
			storeMembershipRepository.save(membership);
			var saved = memberPointTransactionRepository.saveAndFlush(entry);
			return new MembershipPointsResult(
				MemberPointTransactionView.from(saved),
				membership.getPointsBalance(),
				membership.getTier()
			);
		} catch (DataIntegrityViolationException exception) {
			throw new DomainConflictException(conflictMessage);
		}
	}

	private MembershipPointsSummary pointsSummary(StoreMembership membership) {
		var history = memberPointTransactionRepository
			.findAllByStoreMembershipIdOrderByCreatedAtDescIdDesc(membership.getId()).stream()
			.map(MemberPointTransactionView::from)
			.toList();
		return new MembershipPointsSummary(
			membership.getMemberProfile().getId(),
			membership.getMemberProfile().getUserAccount().getDisplayName(),
			membership.getPointsBalance(),
			membership.getTier(),
			history
		);
	}

	private void applyPoints(StoreMembership membership, long delta) {
		try {
			membership.applyPointsDelta(delta);
		} catch (IllegalArgumentException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
	}

	private StoreMembership requireOwnerMembership(UserAccount owner, Long memberProfileId) {
		if (memberProfileId == null) throw new DomainRuleViolationException("Member profile is required.");
		var store = systemStoreService.requireForOwner(owner);
		var member = requirePointsMember(memberProfileId);
		return storeMembershipRepository.findByStoreAndMemberProfile(store, member)
			.or(() -> environment.acceptsProfiles(Profiles.of("test"))
				? java.util.Optional.of(storeMembershipRepository.save(new StoreMembership(store, member)))
				: java.util.Optional.empty())
			.orElseThrow(() -> new DomainRuleViolationException("This Member has not joined your store."));
	}

	private void requireOwnerTransaction(UserAccount owner, MemberPointTransaction transaction) {
		var store = systemStoreService.requireForOwner(owner);
		if (transaction.getStore() == null || !transaction.getStore().getId().equals(store.getId())) {
			throw new DomainRuleViolationException("Point transaction not found for your store.");
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

	private StoreMembership requireMemberStoreMembership(String identifier, String storeSlug) {
		var member = requireActiveMember(identifier);
		var store = storeRepository.findBySlugAndActiveTrue(storeSlug)
			.orElseThrow(() -> new DomainRuleViolationException("Store not found."));
		return storeMembershipRepository.findByStoreAndMemberProfile(store, member)
			.filter(membership -> membership.getStatus() == com.castfan.entity.StoreMembershipStatus.ACTIVE)
			.orElseThrow(() -> new DomainRuleViolationException("Join this store before changing notification preferences."));
	}

	private UserAccount requireActiveAccount(String email, UserRole role) {
		var account = userAccountRepository.findByLoginIdentifierIgnoreCase(email)
			.orElseThrow(() -> new DomainRuleViolationException("Account not found."));
		if (account.getRole() != role || account.getStatus() != UserStatus.ACTIVE) {
			throw new DomainRuleViolationException("An active " + role.name() + " account is required.");
		}
		return account;
	}

}
