package com.castfan.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.UserAccountRepository;
import com.castfan.service.PrivilegedAccountProvisioningService;
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
class RoleAuthorizationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PrivilegedAccountProvisioningService provisioningService;

	@Test
	void anonymousUsersAreSentToSignIn() throws Exception {
		mockMvc.perform(get("/member/schedule"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/member/notifications"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/cast"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));

		mockMvc.perform(get("/owner"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));
	}

	@Test
	void memberCannotOpenPrivilegedWorkspaces() throws Exception {
		var member = saveAccount("member-role@castfan.test", UserRole.MEMBER);

		mockMvc.perform(get("/cast").with(user(member.getEmail()).roles("MEMBER")))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/owner").with(user(member.getEmail()).roles("MEMBER")))
			.andExpect(status().isForbidden());
	}

	@Test
	void memberAndCastCanOpenThePublicSchedule() throws Exception {
		var member = saveAccount("member-schedule@castfan.test", UserRole.MEMBER);
		var cast = saveAccount("cast-schedule@castfan.test", UserRole.CAST);

		mockMvc.perform(get("/member/schedule").with(user(member.getEmail()).roles("MEMBER")))
			.andExpect(status().isOk())
			.andExpect(view().name("member/schedule"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Approved Shifts")));
		mockMvc.perform(get("/member/schedule").param("view", "week")
				.with(user(member.getEmail()).roles("MEMBER")))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Previous week")));
		mockMvc.perform(get("/member/schedule").param("view", "year")
				.with(user(member.getEmail()).roles("MEMBER")))
			.andExpect(status().isBadRequest());
		mockMvc.perform(get("/member/schedule").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isOk())
			.andExpect(view().name("member/schedule"));
	}

	@Test
	void castCanOpenOnlyTheCastWorkspace() throws Exception {
		var cast = provisioningService.provisionCast(
			"cast-role@castfan.test", "secure-cast-pass-123", "Cast Role", "Role Cast"
		);

		mockMvc.perform(get("/cast").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isOk())
			.andExpect(view().name("cast/dashboard"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Role Cast")));
		mockMvc.perform(get("/member/following").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isOk())
			.andExpect(view().name("member/following"));
		mockMvc.perform(get("/s/castfan/account").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/owner").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isForbidden());
	}

	@Test
	void castCanOpenNotificationsButOwnerCannot() throws Exception {
		var cast = provisioningService.provisionCast(
			"cast-notifications@castfan.test", "secure-cast-pass-123", "Cast Notifications", "Notifications Cast"
		);
		var owner = saveAccount("owner-notifications@castfan.test", UserRole.OWNER);

		mockMvc.perform(get("/member/notifications").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isOk())
			.andExpect(view().name("member/notifications"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Notifications")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("aria-label=\"Cast navigation\"")));

		mockMvc.perform(get("/member/notifications").with(user(owner.getEmail()).roles("OWNER")))
			.andExpect(status().isForbidden());
	}

	@Test
	void ownerCanOpenOnlyTheOwnerWorkspace() throws Exception {
		var owner = saveAccount("owner-role@castfan.test", UserRole.OWNER);

		mockMvc.perform(get("/owner").with(user(owner.getEmail()).roles("OWNER")))
			.andExpect(status().isOk())
			.andExpect(view().name("owner/dashboard"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("CastFan operations")));
		mockMvc.perform(get("/cast").with(user(owner.getEmail()).roles("OWNER")))
			.andExpect(status().isForbidden());
	}

	private UserAccount saveAccount(String email, UserRole role) {
		return userAccountRepository.saveAndFlush(
			new UserAccount(email, "encoded-password", email, role, UserStatus.ACTIVE)
		);
	}
}
