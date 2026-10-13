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

	@Test
	void anonymousUsersAreSentToSignIn() throws Exception {
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
	void castCanOpenOnlyTheCastWorkspace() throws Exception {
		var cast = saveAccount("cast-role@castfan.test", UserRole.CAST);

		mockMvc.perform(get("/cast").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isOk())
			.andExpect(view().name("workspace/cast"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Cast workspace")));
		mockMvc.perform(get("/owner").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isForbidden());
	}

	@Test
	void ownerCanOpenOnlyTheOwnerWorkspace() throws Exception {
		var owner = saveAccount("owner-role@castfan.test", UserRole.OWNER);

		mockMvc.perform(get("/owner").with(user(owner.getEmail()).roles("OWNER")))
			.andExpect(status().isOk())
			.andExpect(view().name("workspace/owner"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Owner workspace")));
		mockMvc.perform(get("/cast").with(user(owner.getEmail()).roles("OWNER")))
			.andExpect(status().isForbidden());
	}

	private UserAccount saveAccount(String email, UserRole role) {
		return userAccountRepository.saveAndFlush(
			new UserAccount(email, "encoded-password", email, role, UserStatus.ACTIVE)
		);
	}
}
