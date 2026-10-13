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
import com.castfan.entity.MemberProfile;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.MemberTierRepository;
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
			.andExpect(status().isOk())
			.andExpect(view().name("cast/tiers"));
		mockMvc.perform(get("/cast/tiers").with(memberRequest))
			.andExpect(status().isForbidden());

		mockMvc.perform(post("/cast/tiers")
				.with(castRequest)
				.param("name", "Web Tier")
				.param("rank", "1")
				.param("monthlyPrice", "149.00"))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/cast/tiers")
				.with(castRequest)
				.with(csrf())
				.param("name", "Web Tier")
				.param("rank", "1")
				.param("monthlyPrice", "149.00"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/cast/tiers?created"));

		assertThat(memberTierRepository.count()).isEqualTo(1);
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
			saveAccount(email, UserRole.CAST), stageName, null, null, true
		);
	}

	private UserAccount saveAccount(String email, UserRole role) {
		return userAccountRepository.saveAndFlush(
			new UserAccount(email, "encoded-password", email, role, UserStatus.ACTIVE)
		);
	}
}
