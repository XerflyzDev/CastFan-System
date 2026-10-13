package com.castfan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.castfan.dto.AccountView;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.UserAccountRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class AuthFlowTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private MemberProfileRepository memberProfileRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void clearAccounts() {
		memberProfileRepository.deleteAll();
		userAccountRepository.deleteAll();
	}

	@Test
	void loginAndSignupPagesArePublic() throws Exception {
		mockMvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/login"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in")));

		mockMvc.perform(get("/signup"))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/signup"))
			.andExpect(model().attributeExists("form"));
	}

	@Test
	void registrationCreatesAnActiveMemberWithAHashedPassword() throws Exception {
		mockMvc.perform(post("/signup")
				.with(csrf())
				.param("displayName", "Mira Fan")
				.param("email", "MIRA@example.com")
				.param("password", "secure-pass-123")
				.param("confirmPassword", "secure-pass-123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login?registered"));

		var account = userAccountRepository.findByEmailIgnoreCase("mira@example.com").orElseThrow();
		assertThat(account.getEmail()).isEqualTo("mira@example.com");
		assertThat(account.getDisplayName()).isEqualTo("Mira Fan");
		assertThat(account.getRole()).isEqualTo(UserRole.MEMBER);
		assertThat(account.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(account.getPasswordHash()).isNotEqualTo("secure-pass-123");
		assertThat(passwordEncoder.matches("secure-pass-123", account.getPasswordHash())).isTrue();
		assertThat(memberProfileRepository.findByUserAccountId(account.getId())).isPresent();
	}

	@Test
	void registrationRejectsDuplicateEmailIgnoringCase() throws Exception {
		mockMvc.perform(post("/signup")
				.with(csrf())
				.param("displayName", "First Member")
				.param("email", "member@example.com")
				.param("password", "secure-pass-123")
				.param("confirmPassword", "secure-pass-123"))
			.andExpect(status().is3xxRedirection());

		mockMvc.perform(post("/signup")
				.with(csrf())
				.param("displayName", "Second Member")
				.param("email", "MEMBER@example.com")
				.param("password", "another-pass-123")
				.param("confirmPassword", "another-pass-123"))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/signup"))
			.andExpect(model().attributeHasFieldErrors("form", "email"));

		assertThat(userAccountRepository.count()).isEqualTo(1);
	}

	@Test
	void registrationRejectsPasswordLongerThanBcryptByteLimit() throws Exception {
		var oversizedPassword = "\u00e9".repeat(40);

		mockMvc.perform(post("/signup")
				.with(csrf())
				.param("displayName", "Unicode Member")
				.param("email", "unicode@example.com")
				.param("password", oversizedPassword)
				.param("confirmPassword", oversizedPassword))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/signup"))
			.andExpect(model().attributeHasFieldErrors("form", "password"));

		assertThat(userAccountRepository.count()).isZero();
	}

	@Test
	void accountPageRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/account"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));
	}

	@Test
	void registeredMemberCanSignInAndOpenTheAccountPage() throws Exception {
		mockMvc.perform(post("/signup")
				.with(csrf())
				.param("displayName", "Signed In Member")
				.param("email", "signed-in@example.com")
				.param("password", "secure-pass-123")
				.param("confirmPassword", "secure-pass-123"))
			.andExpect(status().is3xxRedirection());

		HttpSession session = mockMvc.perform(post("/login")
				.with(csrf())
				.param("email", "signed-in@example.com")
				.param("password", "secure-pass-123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/account"))
			.andReturn()
			.getRequest()
			.getSession(false);

		assertThat(session).isNotNull();
		mockMvc.perform(get("/account").session((org.springframework.mock.web.MockHttpSession) session))
			.andExpect(status().isOk())
			.andExpect(view().name("account/index"))
			.andExpect(model().attribute("account", instanceOf(AccountView.class)))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Signed In Member")));
	}
}
