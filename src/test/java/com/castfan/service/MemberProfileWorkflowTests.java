package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.castfan.entity.MemberProfile;
import com.castfan.entity.Store;
import com.castfan.entity.StoreMembership;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.StoreMembershipRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberProfileWorkflowTests {

	@Autowired private MockMvc mockMvc;
	@Autowired private UserAccountRepository userAccountRepository;
	@Autowired private MemberProfileRepository memberProfileRepository;
	@Autowired private StoreMembershipRepository storeMembershipRepository;
	@Autowired private StoreRepository storeRepository;
	@Autowired private SystemStoreService systemStoreService;
	@Autowired private ProfileService profileService;

	private UserAccount member;
	private MemberProfile memberProfile;

	@BeforeEach
	void setUp() {
		var suffix = Long.toString(System.nanoTime());
		member = userAccountRepository.saveAndFlush(new UserAccount(
			"profile-member-" + suffix + "@castfan.test", "encoded", "Original Member",
			UserRole.MEMBER, UserStatus.ACTIVE
		));
		memberProfile = profileService.createMemberProfile(member);
		var store = systemStoreService.findActive().orElseThrow();
		storeMembershipRepository.save(new StoreMembership(store, memberProfile));
	}

	@Test
	void memberAccountCanStayScopedToTheStoreTheyAreViewing() throws Exception {
		var store = createStoreMembership(250);

		mockMvc.perform(get("/s/{slug}/account", store.getSlug()).with(memberUser()))
			.andExpect(status().isOk())
			.andExpect(view().name("account/index"))
			.andExpect(content().string(containsString(store.getName())))
			.andExpect(content().string(containsString("<strong>250</strong>")))
			.andExpect(content().string(containsString("href=\"/s/" + store.getSlug() + "/account\"")))
			.andExpect(content().string(containsString("href=\"/s/" + store.getSlug() + "/home\"")))
			.andExpect(content().string(containsString("href=\"/s/" + store.getSlug() + "/schedule\"")))
			.andExpect(content().string(containsString("href=\"/s/" + store.getSlug() + "/following\"")))
			.andExpect(content().string(containsString("href=\"/s/" + store.getSlug() + "/membership\"")))
			.andExpect(content().string(containsString(
				"href=\"/s/" + store.getSlug() + "/notification-settings\"")));
	}

	@Test
	void storefrontProfileLinkKeepsTheStoreContext() throws Exception {
		var store = createStoreMembership(0);

		mockMvc.perform(get("/s/{slug}/home", store.getSlug()).with(memberUser()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("href=\"/s/" + store.getSlug() + "/account\"")));
	}

	@Test
	void memberProfileEditKeepsTheStoreContext() throws Exception {
		var store = createStoreMembership(0);

		mockMvc.perform(get("/member/profile").param("store", store.getSlug()).with(memberUser()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("href=\"/s/" + store.getSlug() + "/account\"")))
			.andExpect(content().string(containsString(
				"action=\"/member/profile?store=" + store.getSlug() + "\"")));
	}

	@Test
	void memberCanOpenAndUpdateOwnProfile() throws Exception {
		mockMvc.perform(get("/member/profile").with(memberUser()))
			.andExpect(status().isOk()).andExpect(view().name("member/profile"))
			.andExpect(content().string(containsString("Original Member")))
			.andExpect(content().string(containsString("Profile details")))
			.andExpect(content().string(containsString("Save profile")))
			.andExpect(content().string(org.hamcrest.Matchers.not(containsString("Notification preferences"))));

		var image = new MockMultipartFile("image", "avatar.png", "image/png",
			new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1});
		mockMvc.perform(multipart("/member/profile").file(image).with(memberUser()).with(csrf())
				.param("displayName", "Aruna Fan")
				.param("bio", "I follow the evening Cast team.")
				.param("castUpdateNotifications", "true")
				.param("eventNotifications", "true"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/member/profile?saved"));

		var updatedAccount = userAccountRepository.findById(member.getId()).orElseThrow();
		var updatedProfile = memberProfileRepository.findByUserAccountId(member.getId()).orElseThrow();
		assertThat(updatedAccount.getDisplayName()).isEqualTo("Aruna Fan");
		assertThat(updatedProfile.getBio()).isEqualTo("I follow the evening Cast team.");
		assertThat(updatedProfile.getAvatarUrl()).isEqualTo("/avatars/" + member.getId());
		assertThat(updatedProfile.isCastUpdateNotifications()).isTrue();
		assertThat(updatedProfile.isEventNotifications()).isTrue();
		mockMvc.perform(get("/account").with(memberUser()))
			.andExpect(content().string(containsString("I follow the evening Cast team.")))
			.andExpect(content().string(org.hamcrest.Matchers.not(containsString("Owner navigation"))))
			.andExpect(content().string(containsString("/account/avatar")))
			.andExpect(content().string(containsString("Membership points")))
			.andExpect(content().string(containsString("No tier yet")))
			.andExpect(content().string(containsString("class=\"membership-card\"")))
			.andExpect(content().string(containsString("<strong>0</strong>")))
			.andExpect(content().string(containsString("<small>POINTS</small>")))
			.andExpect(content().string(org.hamcrest.Matchers.not(containsString("No active memberships"))));
		mockMvc.perform(get("/avatars/{id}", member.getId()))
			.andExpect(status().isOk()).andExpect(content().contentType("image/png"));
	}

	@Test
	void memberCanOpenDedicatedNotificationSettings() throws Exception {
		mockMvc.perform(get("/member/notification-settings").with(memberUser()))
			.andExpect(status().isOk())
			.andExpect(view().name("member/notification-settings"))
			.andExpect(content().string(containsString("Choose your updates")))
			.andExpect(content().string(containsString("Changes are saved automatically")))
			.andExpect(content().string(org.hamcrest.Matchers.not(containsString("Display name"))))
			.andExpect(content().string(org.hamcrest.Matchers.not(containsString("Profile photo"))));
	}

	@Test
	void invalidProfileIsRejectedWithoutChangingStoredDetails() throws Exception {
		mockMvc.perform(post("/member/profile").with(memberUser()).with(csrf())
				.param("displayName", " ")
				.param("bio", "Rejected bio"))
			.andExpect(status().isOk()).andExpect(view().name("member/profile"))
			.andExpect(content().string(containsString("Display name is required")));

		var stored = memberProfileRepository.findByUserAccountId(member.getId()).orElseThrow();
		assertThat(stored.getBio()).isNull();
		assertThat(stored.getAvatarUrl()).isNull();
	}

	@Test
	void profileUpdateRequiresMemberRoleAndCsrf() throws Exception {
		var cast = userAccountRepository.saveAndFlush(new UserAccount(
			"profile-cast-" + System.nanoTime() + "@castfan.test", "encoded", "Cast",
			UserRole.CAST, UserStatus.ACTIVE
		));
		mockMvc.perform(get("/member/profile").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/member/profile").with(memberUser())
				.param("displayName", "No CSRF").param("bio", "Rejected"))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/member/profile/notification-preferences")
				.with(user(cast.getEmail()).roles("CAST")).with(csrf())
				.param("preference", "eventNotifications")
				.param("enabled", "false"))
			.andExpect(status().isForbidden());
		assertThat(userAccountRepository.findById(member.getId()).orElseThrow().getDisplayName())
			.isEqualTo("Original Member");
	}

	@Test
	void memberCanUpdateNotificationPreferencesIndependently() throws Exception {
		mockMvc.perform(post("/member/profile/notification-preferences")
				.with(memberUser()).with(csrf())
				.param("preference", "castUpdateNotifications")
				.param("enabled", "false"))
			.andExpect(status().isNoContent());

		var stored = memberProfileRepository.findByUserAccountId(member.getId()).orElseThrow();
		assertThat(stored.isCastUpdateNotifications()).isFalse();
		assertThat(stored.isEventNotifications()).isTrue();

		mockMvc.perform(post("/member/profile/notification-preferences")
				.with(memberUser()).with(csrf())
				.param("preference", "eventNotifications")
				.param("enabled", "false"))
			.andExpect(status().isNoContent());

		assertThat(memberProfileRepository.findByUserAccountId(member.getId()).orElseThrow().isEventNotifications())
			.isFalse();
	}

	@Test
	void notificationPreferenceUpdateRejectsInvalidRequests() throws Exception {
		mockMvc.perform(post("/member/profile/notification-preferences")
				.with(memberUser()).with(csrf())
				.param("preference", "unsupportedPreference")
				.param("enabled", "false"))
			.andExpect(status().isBadRequest());

		mockMvc.perform(post("/member/profile/notification-preferences")
				.with(memberUser())
				.param("preference", "eventNotifications")
				.param("enabled", "false"))
			.andExpect(status().isForbidden());
	}

	private Store createStoreMembership(long points) {
		var suffix = Long.toString(System.nanoTime());
		var owner = userAccountRepository.saveAndFlush(new UserAccount(
			"store-owner-" + suffix + "@castfan.test", "encoded", "Store Owner",
			UserRole.OWNER, UserStatus.ACTIVE
		));
		var store = storeRepository.saveAndFlush(new Store(
			owner, "Aizen Test Cafe " + suffix, "aizen-test-" + suffix, null, null, "Asia/Bangkok", true
		));
		var membership = new StoreMembership(store, memberProfile);
		membership.applyPointsDelta(points);
		storeMembershipRepository.saveAndFlush(membership);
		return store;
	}

	private UserRequestPostProcessor memberUser() {
		return user(member.getEmail()).roles("MEMBER");
	}
}
