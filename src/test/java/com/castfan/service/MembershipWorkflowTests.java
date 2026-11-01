package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;

import com.castfan.dto.MemberTierForm;
import com.castfan.entity.CastProfile;
import com.castfan.entity.MemberPointTransactionType;
import com.castfan.entity.MemberProfile;
import com.castfan.entity.StoreMembershipTier;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.MemberTierRepository;
import com.castfan.repository.MemberPointTransactionRepository;
import com.castfan.repository.TierMembershipRepository;
import com.castfan.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MembershipWorkflowTests {

	@Autowired
	private MembershipService membershipService;

	@Autowired
	private ProfileService profileService;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private MemberTierRepository memberTierRepository;

	@Autowired
	private TierMembershipRepository tierMembershipRepository;

	@Autowired
	private MemberPointTransactionRepository memberPointTransactionRepository;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void castCreatesOrderedTiersWithNormalizedUniqueNamesAndRanks() {
		var cast = createCast("tier-cast@castfan.test", "Tier Cast");

		var first = membershipService.createTier(cast.getUserAccount().getEmail(), tierForm("Silver", 1, "199.00"));
		var second = membershipService.createTier(cast.getUserAccount().getEmail(), tierForm("Gold", 2, "399.00"));

		assertThat(first.currency()).isEqualTo("THB");
		assertThat(first.monthlyPrice()).isEqualByComparingTo("199.00");
		assertThat(membershipService.ownedTiers(cast.getUserAccount().getEmail()))
			.extracting(view -> view.name())
			.containsExactly("Silver", "Gold");
		assertThatThrownBy(() ->
			membershipService.createTier(cast.getUserAccount().getEmail(), tierForm("SILVER", 3, "499.00"))
		).isInstanceOf(DomainConflictException.class);
		assertThatThrownBy(() ->
			membershipService.createTier(cast.getUserAccount().getEmail(), tierForm("Platinum", 2, "799.00"))
		).isInstanceOf(DomainConflictException.class);
		assertThat(second.rank()).isEqualTo(2);
	}

	@Test
	void tierCreationRejectsInvalidBusinessValuesOutsideTheController() {
		var cast = createCast("invalid-tier-cast@castfan.test", "Invalid Tier Cast");
		var invalidPrice = tierForm("Invalid", 1, "-1.00");

		assertThatThrownBy(() -> membershipService.createTier(cast.getUserAccount().getEmail(), invalidPrice))
			.isInstanceOf(DomainRuleViolationException.class);
	}

	@Test
	void memberCanHaveOnlyOneActiveTierPerCastAndSubscriptionIsIdempotent() {
		var member = createMember("tier-member@castfan.test");
		var cast = createCast("subscription-cast@castfan.test", "Subscription Cast");
		var silver = membershipService.createTier(cast.getUserAccount().getEmail(), tierForm("Silver", 1, "199.00"));
		var gold = membershipService.createTier(cast.getUserAccount().getEmail(), tierForm("Gold", 2, "399.00"));

		var firstSubscription = membershipService.subscribe(member.getUserAccount().getEmail(), silver.id());
		var repeatedSubscription = membershipService.subscribe(member.getUserAccount().getEmail(), silver.id());

		assertThat(repeatedSubscription.id()).isEqualTo(firstSubscription.id());
		assertThat(tierMembershipRepository.count()).isEqualTo(1);
		assertThatThrownBy(() -> membershipService.subscribe(member.getUserAccount().getEmail(), gold.id()))
			.isInstanceOf(DomainConflictException.class);
	}

	@Test
	void memberCanCancelReactivateAndCannotCancelAnotherMembersMembership() {
		var firstMember = createMember("cancel-first-member@castfan.test");
		var secondMember = createMember("cancel-second-member@castfan.test");
		var cast = createCast("cancel-tier-cast@castfan.test", "Cancel Tier Cast");
		var tier = membershipService.createTier(cast.getUserAccount().getEmail(), tierForm("Supporter", 1, "99.00"));
		var membership = membershipService.subscribe(firstMember.getUserAccount().getEmail(), tier.id());

		assertThatThrownBy(() -> membershipService.cancel(secondMember.getUserAccount().getEmail(), membership.id()))
			.isInstanceOf(DomainRuleViolationException.class);
		membershipService.cancel(firstMember.getUserAccount().getEmail(), membership.id());
		assertThat(membershipService.activeMemberships(firstMember.getUserAccount().getEmail())).isEmpty();

		var reactivated = membershipService.subscribe(firstMember.getUserAccount().getEmail(), tier.id());
		assertThat(reactivated.id()).isEqualTo(membership.id());
	}

	@Test
	void accessCheckUsesTheActiveTierRank() {
		var member = createMember("access-member@castfan.test");
		var cast = createCast("access-cast@castfan.test", "Access Cast");
		var tier = membershipService.createTier(cast.getUserAccount().getEmail(), tierForm("Gold", 2, "399.00"));

		assertThat(membershipService.hasTierAccess(member.getUserAccount().getEmail(), cast.getId(), 1)).isFalse();
		membershipService.subscribe(member.getUserAccount().getEmail(), tier.id());
		assertThat(membershipService.hasTierAccess(member.getUserAccount().getEmail(), cast.getId(), 1)).isTrue();
		assertThat(membershipService.hasTierAccess(member.getUserAccount().getEmail(), cast.getId(), 2)).isTrue();
		assertThat(membershipService.hasTierAccess(member.getUserAccount().getEmail(), cast.getId(), 3)).isFalse();
	}

	@Test
	void membershipPagesEnforceRolesAndTierCreationRequiresCsrf() throws Exception {
		var member = createMember("membership-web-member@castfan.test");
		var cast = createCast("membership-web-cast@castfan.test", "Membership Web Cast");
		var memberRequest = user(member.getUserAccount().getEmail()).roles("MEMBER");
		var castRequest = user(cast.getUserAccount().getEmail()).roles("CAST");

		mockMvc.perform(get("/member/memberships").with(memberRequest))
			.andExpect(status().isOk())
			.andExpect(view().name("member/memberships"));
		mockMvc.perform(get("/member/memberships").with(castRequest))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/cast/tiers").with(castRequest))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/cast/content?legacyTiers"));
		mockMvc.perform(get("/cast/tiers").with(memberRequest))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/cast/tiers").with(castRequest).with(csrf()))
			.andExpect(status().isMethodNotAllowed());
	}

	@Test
	void newMembersStartAtZeroAndTierThresholdsUseTheAccumulatedBalance() {
		var owner = saveAccount("points-owner@castfan.test", UserRole.OWNER);
		var member = createMember("points-member@castfan.test");

		assertThat(member.getPointsBalance()).isZero();
		assertThat(member.getStoreMembershipTier()).isEqualTo(StoreMembershipTier.NONE);

		var first = membershipService.recordPurchasePoints(
			owner.getEmail(), member.getId(), new BigDecimal("1.99"), "R-0001"
		);
		assertThat(first.pointsBalance()).isEqualTo(1);
		assertThat(first.tier()).isEqualTo(StoreMembershipTier.TIER_1);

		var tierOneMaximum = membershipService.recordPurchasePoints(
			owner.getEmail(), member.getId(), new BigDecimal("4998.99"), "R-0002"
		);
		assertThat(tierOneMaximum.pointsBalance()).isEqualTo(4_999);
		assertThat(tierOneMaximum.tier()).isEqualTo(StoreMembershipTier.TIER_1);

		var tierTwoMinimum = membershipService.recordPurchasePoints(
			owner.getEmail(), member.getId(), BigDecimal.ONE, "R-0003"
		);
		assertThat(tierTwoMinimum.pointsBalance()).isEqualTo(5_000);
		assertThat(tierTwoMinimum.tier()).isEqualTo(StoreMembershipTier.TIER_2);

		var tierTwoMaximum = membershipService.recordPurchasePoints(
			owner.getEmail(), member.getId(), new BigDecimal("14999"), "R-0004"
		);
		assertThat(tierTwoMaximum.pointsBalance()).isEqualTo(19_999);
		assertThat(tierTwoMaximum.tier()).isEqualTo(StoreMembershipTier.TIER_2);

		var tierThreeMinimum = membershipService.recordPurchasePoints(
			owner.getEmail(), member.getId(), BigDecimal.ONE, "R-0005"
		);
		assertThat(tierThreeMinimum.pointsBalance()).isEqualTo(20_000);
		assertThat(tierThreeMinimum.tier()).isEqualTo(StoreMembershipTier.TIER_3);
	}

	@Test
	void previewFloorsFractionalThbWithoutMutatingTheBalance() {
		var owner = saveAccount("preview-owner@castfan.test", UserRole.OWNER);
		var member = createMember("preview-member@castfan.test");

		var preview = membershipService.previewEarnPoints(
			owner.getEmail(), member.getId(), new BigDecimal("199.75"), "PREVIEW-1"
		);

		assertThat(preview.pointsDelta()).isEqualTo(199);
		assertThat(preview.resultingBalance()).isEqualTo(199);
		assertThat(member.getPointsBalance()).isZero();
		assertThat(memberPointTransactionRepository.count()).isZero();
	}

	@Test
	void onlyAnActiveOwnerCanRecordPoints() {
		var member = createMember("authorization-member@castfan.test");
		var cast = createCast("authorization-cast@castfan.test", "Authorization Cast");
		var suspendedOwner = userAccountRepository.saveAndFlush(new UserAccount(
			"suspended-owner@castfan.test",
			"encoded-password",
			"Suspended Owner",
			UserRole.OWNER,
			UserStatus.SUSPENDED
		));

		assertThatThrownBy(() -> membershipService.recordPurchasePoints(
			member.getUserAccount().getEmail(), member.getId(), BigDecimal.TEN, "AUTH-1"
		)).isInstanceOf(DomainRuleViolationException.class);
		assertThatThrownBy(() -> membershipService.recordPurchasePoints(
			cast.getUserAccount().getEmail(), member.getId(), BigDecimal.TEN, "AUTH-2"
		)).isInstanceOf(DomainRuleViolationException.class);
		assertThatThrownBy(() -> membershipService.recordPurchasePoints(
			suspendedOwner.getEmail(), member.getId(), BigDecimal.TEN, "AUTH-3"
		)).isInstanceOf(DomainRuleViolationException.class);
		assertThat(memberPointTransactionRepository.count()).isZero();
	}

	@Test
	void receiptReferenceIsRequiredNormalizedAndUnique() {
		var owner = saveAccount("receipt-owner@castfan.test", UserRole.OWNER);
		var firstMember = createMember("receipt-first@castfan.test");
		var secondMember = createMember("receipt-second@castfan.test");

		assertThatThrownBy(() -> membershipService.recordPurchasePoints(
			owner.getEmail(), firstMember.getId(), BigDecimal.TEN, " "
		)).isInstanceOf(DomainRuleViolationException.class);

		membershipService.recordPurchasePoints(
			owner.getEmail(), firstMember.getId(), BigDecimal.TEN, " Receipt-100 "
		);
		assertThatThrownBy(() -> membershipService.recordPurchasePoints(
			owner.getEmail(), secondMember.getId(), BigDecimal.TEN, "receipt-100"
		)).isInstanceOf(DomainConflictException.class);
	}

	@Test
	void reversalAppendsAnImmutableEntryAndDowngradesImmediately() {
		var owner = saveAccount("reversal-owner@castfan.test", UserRole.OWNER);
		var member = createMember("reversal-member@castfan.test");
		var earned = membershipService.recordPurchasePoints(
			owner.getEmail(), member.getId(), new BigDecimal("5000"), "REV-1"
		);

		var reversed = membershipService.reversePurchasePoints(
			owner.getEmail(), earned.transaction().id(), "Customer refund"
		);

		assertThat(reversed.pointsBalance()).isZero();
		assertThat(reversed.tier()).isEqualTo(StoreMembershipTier.NONE);
		assertThat(reversed.transaction().type()).isEqualTo(MemberPointTransactionType.REVERSAL);
		assertThat(reversed.transaction().pointsDelta()).isEqualTo(-5_000);
		assertThat(reversed.transaction().originalTransactionId()).isEqualTo(earned.transaction().id());
		var original = memberPointTransactionRepository.findById(earned.transaction().id()).orElseThrow();
		assertThat(original.getType()).isEqualTo(MemberPointTransactionType.EARN);
		assertThat(original.getPointsDelta()).isEqualTo(5_000);
		assertThat(original.getReceiptReference()).isEqualTo("REV-1");
		assertThat(memberPointTransactionRepository.count()).isEqualTo(2);
		assertThatThrownBy(() -> membershipService.reversePurchasePoints(
			owner.getEmail(), earned.transaction().id(), "Duplicate refund"
		)).isInstanceOf(DomainConflictException.class);
	}

	@Test
	void correctionRequiresAReasonPreservesHistoryAndPreventsNegativeBalances() {
		var owner = saveAccount("correction-owner@castfan.test", UserRole.OWNER);
		var member = createMember("correction-member@castfan.test");
		var earned = membershipService.recordPurchasePoints(
			owner.getEmail(), member.getId(), new BigDecimal("5000.99"), "COR-1"
		);

		assertThatThrownBy(() -> membershipService.correctPurchasePoints(
			owner.getEmail(), earned.transaction().id(), new BigDecimal("4999"), " "
		)).isInstanceOf(DomainRuleViolationException.class);

		var corrected = membershipService.correctPurchasePoints(
			owner.getEmail(), earned.transaction().id(), new BigDecimal("4999.99"), "Bill amount corrected"
		);
		assertThat(corrected.transaction().type()).isEqualTo(MemberPointTransactionType.CORRECTION);
		assertThat(corrected.transaction().pointsDelta()).isEqualTo(-1);
		assertThat(corrected.pointsBalance()).isEqualTo(4_999);
		assertThat(corrected.tier()).isEqualTo(StoreMembershipTier.TIER_1);
		assertThat(memberPointTransactionRepository.count()).isEqualTo(2);
		assertThatThrownBy(() -> member.applyPointsDelta(-5_000))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Point balance cannot be negative.");
	}

	@Test
	void memberHistoryIsReadOnlyAndStoreTierAccessUsesPointsBalance() {
		var owner = saveAccount("history-owner@castfan.test", UserRole.OWNER);
		var member = createMember("history-member@castfan.test");
		membershipService.recordPurchasePoints(
			owner.getEmail(), member.getId(), new BigDecimal("5000"), "HISTORY-1"
		);

		var summary = membershipService.memberPoints(member.getUserAccount().getEmail());
		assertThat(summary.pointsBalance()).isEqualTo(5_000);
		assertThat(summary.tier()).isEqualTo(StoreMembershipTier.TIER_2);
		assertThat(summary.transactions()).hasSize(1);
		assertThat(membershipService.hasStoreTierAccess(member.getUserAccount().getEmail(), 2)).isTrue();
		assertThat(membershipService.hasStoreTierAccess(member.getUserAccount().getEmail(), 3)).isFalse();
	}

	private MemberTierForm tierForm(String name, int rank, String price) {
		var form = new MemberTierForm();
		form.setName(name);
		form.setDescription(name + " membership benefits");
		form.setRank(rank);
		form.setMonthlyPrice(new BigDecimal(price));
		return form;
	}

	private MemberProfile createMember(String email) {
		return profileService.createMemberProfile(saveAccount(email, UserRole.MEMBER));
	}

	private CastProfile createCast(String email, String stageName) {
		return profileService.createCastProfile(
			saveAccount(email, UserRole.CAST), stageName, null, null
		);
	}

	private UserAccount saveAccount(String email, UserRole role) {
		return userAccountRepository.saveAndFlush(
			new UserAccount(email, "encoded-password", email, role, UserStatus.ACTIVE)
		);
	}
}
