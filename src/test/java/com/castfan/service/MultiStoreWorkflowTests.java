package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;

import com.castfan.dto.OwnerCastCreateForm;
import com.castfan.dto.RegistrationForm;
import com.castfan.dto.StoreSignupForm;
import com.castfan.entity.Store;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreMembershipRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MultiStoreWorkflowTests {
	@Autowired StoreSignupService storeSignupService;
	@Autowired RegistrationService registrationService;
	@Autowired StoreEntryService storeEntryService;
	@Autowired MembershipService membershipService;
	@Autowired OwnerStoreService ownerStoreService;
	@Autowired StoreRepository stores;
	@Autowired UserAccountRepository accounts;
	@Autowired MemberProfileRepository members;
	@Autowired StoreMembershipRepository memberships;
	@Autowired StoreCastRepository assignments;
	@Autowired CastProfileRepository casts;
	@Autowired PasswordEncoder passwordEncoder;
	@Autowired JdbcTemplate jdbcTemplate;
	@Autowired MockMvc mockMvc;

	@Test
	void memberAccountKeepsIndependentMembershipPointsAndReceiptsPerStore() {
		var suffix = String.valueOf(System.nanoTime());
		var first = storeSignupService.register(storeForm("Alpha Owner", "alpha-" + suffix + "@test.local", "alpha-" + suffix));
		var second = storeSignupService.register(storeForm("Beta Owner", "beta-" + suffix + "@test.local", "beta-" + suffix));
		var memberAccount = registrationService.registerMember(memberForm("member-" + suffix + "@test.local"));

		assertThat(storeEntryService.joinAndHome(memberAccount, first.getSlug())).isEqualTo("/s/" + first.getSlug() + "/home");
		storeEntryService.joinAndHome(memberAccount, first.getSlug());
		storeEntryService.joinAndHome(memberAccount, second.getSlug());

		var profile = members.findByUserAccountId(memberAccount.getId()).orElseThrow();
		assertThat(memberships.findAllByMemberProfileOrderByJoinedAtAsc(profile)).hasSize(2);
		membershipService.recordPurchasePoints(first.getOwner().getLoginIdentifier(), profile.getId(), new BigDecimal("100"), "SAME-RECEIPT");
		membershipService.recordPurchasePoints(second.getOwner().getLoginIdentifier(), profile.getId(), new BigDecimal("250"), "SAME-RECEIPT");

		assertThat(membershipService.memberPoints(memberAccount.getLoginIdentifier(), first.getSlug()).pointsBalance()).isEqualTo(100);
		assertThat(membershipService.memberPoints(memberAccount.getLoginIdentifier(), second.getSlug()).pointsBalance()).isEqualTo(250);
		assertThat(membershipService.memberPoints(memberAccount.getLoginIdentifier(), first.getSlug()).transactions()).hasSize(1);
	}

	@Test
	void ownerCreatesStoreSpecificCastCredentialsAndAnotherOwnerCannotResetThem() {
		var suffix = String.valueOf(System.nanoTime());
		var first = storeSignupService.register(storeForm("First Owner", "owner1-" + suffix + "@test.local", "first-" + suffix));
		var second = storeSignupService.register(storeForm("Second Owner", "owner2-" + suffix + "@test.local", "second-" + suffix));
		var form = new OwnerCastCreateForm(); form.setDisplayName("Mina Example"); form.setStageName("Mina");

		var credentials = ownerStoreService.createCast(first.getOwner().getLoginIdentifier(), form);
		var castAccount = accounts.findByLoginIdentifierIgnoreCase(credentials.castId()).orElseThrow();
		var profile = casts.findByUserAccountId(castAccount.getId()).orElseThrow();
		var assignment = assignments.findByStoreIdAndCastProfileId(first.getId(), profile.getId()).orElseThrow();

		assertThat(credentials.castId()).startsWith("mina@").endsWith(first.getSlug());
		assertThat(castAccount.getEmail()).isNull();
		assertThat(castAccount.isMustChangePassword()).isTrue();
		assertThat(passwordEncoder.matches(credentials.temporaryPassword(), castAccount.getPasswordHash())).isTrue();
		assertThat(profile.getStore().getId()).isEqualTo(first.getId());
		assertThatThrownBy(() -> ownerStoreService.resetCastPassword(second.getOwner().getLoginIdentifier(), assignment.getId()))
			.isInstanceOf(DomainRuleViolationException.class);
	}

	@Test
	void databasePreventsAnOwnerFromOwningTwoStores() {
		var suffix = String.valueOf(System.nanoTime());
		var first = storeSignupService.register(storeForm("One Store Owner", "single-" + suffix + "@test.local", "only-" + suffix));
		assertThatThrownBy(() -> stores.saveAndFlush(new Store(first.getOwner(), "Second Store", "second-" + suffix,
			null, "Bangkok", "Asia/Bangkok", true))).isInstanceOf(RuntimeException.class);
	}

	@Test
	void memberNotificationPreferencesAreIndependentPerStore() {
		var suffix = String.valueOf(System.nanoTime());
		var first = storeSignupService.register(storeForm("First Owner", "prefs1-" + suffix + "@test.local", "prefs-first-" + suffix));
		var second = storeSignupService.register(storeForm("Second Owner", "prefs2-" + suffix + "@test.local", "prefs-second-" + suffix));
		var memberAccount = registrationService.registerMember(memberForm("prefs-member-" + suffix + "@test.local"));
		storeEntryService.joinAndHome(memberAccount, first.getSlug());
		storeEntryService.joinAndHome(memberAccount, second.getSlug());

		membershipService.updateNotificationPreference(memberAccount.getLoginIdentifier(), first.getSlug(),
			"castUpdateNotifications", false);

		assertThat(membershipService.notificationPreferenceForm(memberAccount.getLoginIdentifier(), first.getSlug())
			.isCastUpdateNotifications()).isFalse();
		assertThat(membershipService.notificationPreferenceForm(memberAccount.getLoginIdentifier(), second.getSlug())
			.isCastUpdateNotifications()).isTrue();
	}

	@Test
	void publicStoreQrLandingRendersWithoutAuthentication() throws Exception {
		var suffix = String.valueOf(System.nanoTime());
		var store = storeSignupService.register(storeForm("Landing Owner", "landing-" + suffix + "@test.local",
			"landing-" + suffix));

		mockMvc.perform(get("/s/{slug}", store.getSlug()))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Join this store")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"storefront-hero\"")))
			.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Member navigation"))))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("/s/" + store.getSlug() + "/join")));
	}

	@Test
	void joinedMemberStoreHomeUsesTheStoreAwareMemberShell() throws Exception {
		var suffix = String.valueOf(System.nanoTime());
		var store = storeSignupService.register(storeForm("Home Owner", "home-owner-" + suffix + "@test.local",
			"home-" + suffix));
		var memberAccount = registrationService.registerMember(memberForm("home-member-" + suffix + "@test.local"));
		storeEntryService.joinAndHome(memberAccount, store.getSlug());

		mockMvc.perform(get("/s/{slug}/home", store.getSlug())
				.with(user(memberAccount.getLoginIdentifier()).roles("MEMBER")))
			.andExpect(status().isOk())
			.andExpect(view().name("index"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Good morning,")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Offers for you")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Working Today")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Following updates")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString(store.getName())))
			.andExpect(content().string(org.hamcrest.Matchers.containsString(
				"/s/" + store.getSlug() + "/schedule")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString(
				"/s/" + store.getSlug() + "/following")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString(
				"/s/" + store.getSlug() + "/account")))
			.andExpect(content().string(org.hamcrest.Matchers.not(
				org.hamcrest.Matchers.containsString("storefront-hero"))))
			.andExpect(content().string(org.hamcrest.Matchers.not(
				org.hamcrest.Matchers.containsString("You are viewing your store home"))));
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void myStoresRendersAfterRepositoryTransactionCloses() throws Exception {
		var suffix = String.valueOf(System.nanoTime());
		var store = storeSignupService.register(storeForm("My Stores Owner", "my-stores-owner-" + suffix + "@test.local",
			"my-stores-" + suffix));
		var memberAccount = registrationService.registerMember(memberForm("my-stores-member-" + suffix + "@test.local"));
		var ownerAccountId = store.getOwner().getId();
		try {
			storeEntryService.joinAndHome(memberAccount, store.getSlug());

			mockMvc.perform(get("/account/stores")
					.with(user(memberAccount.getLoginIdentifier()).roles("MEMBER")))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"phone shared-page-shell my-stores-page\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString(store.getName())))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/s/" + store.getSlug() + "/home")));
		}
		finally {
			jdbcTemplate.update("DELETE FROM store_memberships WHERE member_profile_id IN (SELECT id FROM member_profiles WHERE user_account_id = ?)", memberAccount.getId());
			jdbcTemplate.update("DELETE FROM member_profiles WHERE user_account_id = ?", memberAccount.getId());
			jdbcTemplate.update("DELETE FROM stores WHERE id = ?", store.getId());
			jdbcTemplate.update("DELETE FROM user_accounts WHERE id IN (?, ?)", memberAccount.getId(), ownerAccountId);
		}
	}

	private StoreSignupForm storeForm(String owner, String email, String slug) {
		var form = new StoreSignupForm(); form.setOwnerName(owner); form.setEmail(email);
		form.setPassword("secure-owner-pass-123"); form.setConfirmPassword("secure-owner-pass-123");
		form.setStoreName(slug); form.setSlug(slug); form.setDescription("Test store"); form.setAddress("Bangkok"); return form;
	}

	private RegistrationForm memberForm(String email) {
		var form = new RegistrationForm(); form.setDisplayName("Global Member"); form.setEmail(email);
		form.setPassword("secure-member-pass-123"); form.setConfirmPassword("secure-member-pass-123"); return form;
	}
}
