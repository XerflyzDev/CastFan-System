package com.castfan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.castfan.entity.MemberProfile;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.MemberPointTransactionRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.UserAccountRepository;
import com.castfan.service.MembershipService;
import com.castfan.service.ProfileService;
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
class OwnerMemberPointsControllerTests {

	@Autowired private MockMvc mockMvc;
	@Autowired private UserAccountRepository userAccountRepository;
	@Autowired private MemberProfileRepository memberProfileRepository;
	@Autowired private MemberPointTransactionRepository transactionRepository;
	@Autowired private ProfileService profileService;
	@Autowired private MembershipService membershipService;

	@Test
	void ownerCanSearchPreviewFractionalPurchaseAndConfirmPersistedLedger() throws Exception {
		var owner = account("workflow-owner@castfan.test", UserRole.OWNER, "Cafe Owner");
		var member = member("workflow-member@castfan.test", "Mina Member");
		var ownerUser = user(owner.getEmail()).roles("OWNER");

		mockMvc.perform(get("/owner/member-points").param("q", "Mina").with(ownerUser))
			.andExpect(status().isOk())
			.andExpect(view().name("owner/member-points"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Mina Member")));

		mockMvc.perform(post("/owner/member-points/preview").with(ownerUser).with(csrf())
				.param("memberProfileId", member.getId().toString()).param("paidAmount", "199.75")
				.param("receiptReference", "RCPT-WORKFLOW-1"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("199")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Review before recording")));
		assertThat(memberProfileRepository.findById(member.getId()).orElseThrow().getPointsBalance()).isZero();

		var confirmResponse = mockMvc.perform(post("/owner/member-points/confirm").with(ownerUser).with(csrf())
				.param("memberProfileId", member.getId().toString()).param("paidAmount", "199.75")
				.param("receiptReference", "RCPT-WORKFLOW-1"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/member-points?memberId=" + member.getId()))
			.andExpect(flash().attributeExists("success"))
			.andReturn();
		mockMvc.perform(get("/owner/member-points").param("memberId", member.getId().toString())
				.flashAttrs(confirmResponse.getFlashMap()).with(ownerUser))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Points recorded")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Mina Member")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Tier 1")));

		var summary = membershipService.memberPointsForOwner(owner.getEmail(), member.getId());
		assertThat(summary.pointsBalance()).isEqualTo(199);
		assertThat(summary.transactions()).hasSize(1);
		assertThat(summary.transactions().getFirst().pointsDelta()).isEqualTo(199);
		assertThat(summary.transactions().getFirst().createdByDisplayName()).isEqualTo("Cafe Owner");
	}

	@Test
	void invalidAndDuplicateSubmissionsShowErrorsWithoutChangingTheBalanceTwice() throws Exception {
		var owner = account("validation-owner@castfan.test", UserRole.OWNER, "Validation Owner");
		var member = member("validation-member@castfan.test", "Valid Member");
		var ownerUser = user(owner.getEmail()).roles("OWNER");

		mockMvc.perform(post("/owner/member-points/preview").with(ownerUser).with(csrf())
				.param("memberProfileId", member.getId().toString()).param("paidAmount", "0")
				.param("receiptReference", ""))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Paid amount must be greater than zero")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Receipt reference is required")));

		var memberUser = user(member.getUserAccount().getEmail()).roles("MEMBER");
		mockMvc.perform(post("/owner/member-points/confirm").with(memberUser).with(csrf())
				.param("memberProfileId", member.getId().toString()).param("paidAmount", "10")
				.param("receiptReference", "SHOULD-NOT-RECORD"))
			.andExpect(status().isForbidden());

		var cast = account("validation-cast@castfan.test", UserRole.CAST, "Validation Cast");
		mockMvc.perform(get("/owner/member-points").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isForbidden());

		membershipService.recordPurchasePoints(owner.getEmail(), member.getId(), new java.math.BigDecimal("20"),
			"RECEIPT-UNIQUE-OWNER-UI");
		mockMvc.perform(post("/owner/member-points/confirm").with(ownerUser).with(csrf())
				.param("memberProfileId", member.getId().toString()).param("paidAmount", "20")
				.param("receiptReference", "RECEIPT-UNIQUE-OWNER-UI"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("already been used")));

		assertThat(transactionRepository.count()).isEqualTo(1);
		assertThat(memberProfileRepository.findById(member.getId()).orElseThrow().getPointsBalance()).isEqualTo(20);
	}

	@Test
	void ownerPageRequiresAuthenticationAndCannotSelectNonMemberProfiles() throws Exception {
		mockMvc.perform(get("/owner/member-points"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));

		var owner = account("identity-owner@castfan.test", UserRole.OWNER, "Identity Owner");
		var cast = account("identity-cast@castfan.test", UserRole.CAST, "Other Cast");
		mockMvc.perform(post("/owner/member-points/preview").with(user(owner.getEmail()).roles("OWNER")).with(csrf())
				.param("memberProfileId", "999999").param("paidAmount", "50").param("receiptReference", "INVALID-ID"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("That active Member could not be found")));
		assertThat(transactionRepository.count()).isZero();
		assertThat(cast.getRole()).isEqualTo(UserRole.CAST);
	}

	@Test
	void ownerCanPreviewAndConfirmCorrectionAndReverseWithImmediateTierRecalculation() throws Exception {
		var owner = account("adjustment-owner@castfan.test", UserRole.OWNER, "Adjustment Owner");
		var member = member("adjustment-member@castfan.test", "Adjustment Member");
		var ownerUser = user(owner.getEmail()).roles("OWNER");
		var earned = membershipService.recordPurchasePoints(owner.getEmail(), member.getId(),
			new java.math.BigDecimal("5001"), "ADJUSTMENT-ORIGINAL");

		mockMvc.perform(post("/owner/member-points/adjustment/preview").with(ownerUser).with(csrf())
				.param("memberProfileId", member.getId().toString())
				.param("originalTransactionId", earned.transaction().id().toString())
				.param("correctedPaidAmount", "4999.99").param("reason", "Receipt was entered incorrectly"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Correction preview")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Tier 1")));

		mockMvc.perform(post("/owner/member-points/adjustment/confirm").with(ownerUser).with(csrf())
				.param("memberProfileId", member.getId().toString())
				.param("originalTransactionId", earned.transaction().id().toString())
				.param("correctedPaidAmount", "4999.99").param("reason", "Receipt was entered incorrectly"))
			.andExpect(status().is3xxRedirection());
		assertThat(membershipService.memberPointsForOwner(owner.getEmail(), member.getId()).pointsBalance()).isEqualTo(4999);

		var nextEarn = membershipService.recordPurchasePoints(owner.getEmail(), member.getId(),
			new java.math.BigDecimal("1"), "ADJUSTMENT-REVERSAL");
		mockMvc.perform(post("/owner/member-points/reversal").with(ownerUser).with(csrf())
				.param("memberId", member.getId().toString())
				.param("transactionId", nextEarn.transaction().id().toString())
				.param("reason", "Purchase was refunded"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/member-points?memberId=" + member.getId()));

		var summary = membershipService.memberPointsForOwner(owner.getEmail(), member.getId());
		assertThat(summary.pointsBalance()).isEqualTo(4999);
		assertThat(summary.tier()).hasToString("TIER_1");
		assertThat(summary.transactions()).hasSize(4);
		assertThat(summary.transactions()).extracting(transaction -> transaction.originalTransactionId())
			.contains(earned.transaction().id(), nextEarn.transaction().id());
	}

	private MemberProfile member(String email, String displayName) {
		return profileService.createMemberProfile(account(email, UserRole.MEMBER, displayName));
	}

	private UserAccount account(String email, UserRole role, String name) {
		return userAccountRepository.saveAndFlush(new UserAccount(email, "encoded-password", name, role, UserStatus.ACTIVE));
	}
}
