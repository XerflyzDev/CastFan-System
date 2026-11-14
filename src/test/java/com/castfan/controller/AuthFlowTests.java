package com.castfan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.castfan.dto.AccountView;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.AccountAvatarRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import com.castfan.repository.StoreMembershipRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@Transactional
class AuthFlowTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private AccountAvatarRepository accountAvatarRepository;

	@Autowired
	private MemberProfileRepository memberProfileRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private StoreMembershipRepository storeMembershipRepository;

	@Autowired
	private StoreRepository storeRepository;

	@BeforeEach
	void clearAccounts() {
		accountAvatarRepository.deleteAll();
		storeMembershipRepository.deleteAll();
		memberProfileRepository.deleteAll();
		userAccountRepository.deleteAll();
	}

	@Test
	void loginAndSignupPagesArePublic() throws Exception {
		mockMvc.perform(get("/login"))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/login"))
			.andExpect(model().attribute("showGoogleLoginMock", false))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Sign in")));

		mockMvc.perform(get("/signup"))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/signup"))
			.andExpect(model().attributeExists("form"));

		mockMvc.perform(get("/signup/store"))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/store-signup"))
			.andExpect(model().attribute("activeStep", 1))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("data-signup-step=\"1\"")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("data-signup-step=\"2\"")));
	}

	@Test
	void storeSignupReturnsToTheStepContainingValidationErrors() throws Exception {
		mockMvc.perform(post("/signup/store").with(csrf())
				.param("ownerName", "A")
				.param("email", "invalid-email")
				.param("password", "secure-owner-pass-123")
				.param("confirmPassword", "secure-owner-pass-123")
				.param("storeName", "Valid Store")
				.param("address", "Bangkok")
				.param("slug", "valid-store"))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/store-signup"))
			.andExpect(model().attribute("activeStep", 1))
			.andExpect(model().attributeHasFieldErrors("form", "ownerName", "email"));

		mockMvc.perform(post("/signup/store").with(csrf())
				.param("ownerName", "Valid Owner")
				.param("email", "valid-owner@example.com")
				.param("password", "secure-owner-pass-123")
				.param("confirmPassword", "secure-owner-pass-123")
				.param("storeName", "")
				.param("address", "")
				.param("slug", "invalid slug"))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/store-signup"))
			.andExpect(model().attribute("activeStep", 2))
			.andExpect(model().attributeHasFieldErrors("form", "storeName", "address", "slug"));

		assertThat(userAccountRepository.count()).isZero();
		assertThat(storeRepository.count()).isZero();
	}

	@Test
	void storeSignupCreatesOwnerAndStoreOnlyOnFinalSubmitAndRoutesDuplicateErrors() throws Exception {
		mockMvc.perform(post("/signup/store").with(csrf())
				.param("ownerName", "First Owner")
				.param("email", "store-owner@example.com")
				.param("password", "secure-owner-pass-123")
				.param("confirmPassword", "secure-owner-pass-123")
				.param("storeName", "First Store")
				.param("description", "A Cast cafe")
				.param("address", "Bangkok")
				.param("slug", "first-store"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login?storeRegistered"));

		assertThat(userAccountRepository.findByEmailIgnoreCase("store-owner@example.com")).isPresent();
		assertThat(storeRepository.findBySlug("first-store")).isPresent();

		mockMvc.perform(post("/signup/store").with(csrf())
				.param("ownerName", "Duplicate Owner")
				.param("email", "STORE-OWNER@example.com")
				.param("password", "secure-owner-pass-456")
				.param("confirmPassword", "secure-owner-pass-456")
				.param("storeName", "Second Store")
				.param("address", "Bangkok")
				.param("slug", "second-store"))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/store-signup"))
			.andExpect(model().attribute("activeStep", 1))
			.andExpect(model().attributeHasFieldErrors("form", "email"));

		assertThat(userAccountRepository.count()).isEqualTo(1);
		assertThat(storeRepository.count()).isEqualTo(1);
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
	void registrationRejectsDisplayNameShorterThanThreeCharacters() throws Exception {
		mockMvc.perform(post("/signup")
				.with(csrf())
				.param("displayName", "Al")
				.param("email", "short-name@example.com")
				.param("password", "secure-pass-123")
				.param("confirmPassword", "secure-pass-123"))
			.andExpect(status().isOk())
			.andExpect(view().name("auth/signup"))
			.andExpect(model().attributeHasFieldErrors("form", "displayName"));

		assertThat(userAccountRepository.count()).isZero();
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
	void ownerSignsInToThePublicHomePage() throws Exception {
		userAccountRepository.saveAndFlush(new UserAccount(
			"owner-sign-in@castfan.test",
			passwordEncoder.encode("secure-owner-pass-123"),
			"Hoshi Owner",
			UserRole.OWNER,
			UserStatus.ACTIVE
		));

		HttpSession session = mockMvc.perform(post("/login")
				.with(csrf())
				.param("email", "owner-sign-in@castfan.test")
				.param("password", "secure-owner-pass-123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/"))
			.andReturn()
			.getRequest()
			.getSession(false);

		assertThat(session).isNotNull();
		mockMvc.perform(get("/account").session((org.springframework.mock.web.MockHttpSession) session))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Owner navigation")))
			.andExpect(content().string(org.hamcrest.Matchers.not(
				org.hamcrest.Matchers.containsString("Member navigation"))))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("/owner/promotions")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Edit name")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Change profile photo")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Crop photo")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Save photo")));

		mockMvc.perform(post("/account/name").with(csrf())
				.session((org.springframework.mock.web.MockHttpSession) session)
				.param("displayName", "Hoshi Cafe Owner"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/account?nameSaved"));
		assertThat(userAccountRepository.findByEmailIgnoreCase("owner-sign-in@castfan.test").orElseThrow().getDisplayName())
			.isEqualTo("Hoshi Cafe Owner");

		mockMvc.perform(post("/account/name").with(csrf())
				.session((org.springframework.mock.web.MockHttpSession) session)
				.param("displayName", " "))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/account"))
			.andExpect(flash().attribute("nameError", "Name must contain at least 2 characters."));

		var png = new MockMultipartFile(
			"avatar", "owner.png", "image/png",
			new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a}
		);
		mockMvc.perform(multipart("/account/avatar").file(png).with(csrf())
				.session((org.springframework.mock.web.MockHttpSession) session))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/account?photoSaved"));

		mockMvc.perform(get("/account/avatar").session((org.springframework.mock.web.MockHttpSession) session))
			.andExpect(status().isOk())
			.andExpect(content().contentType("image/png"))
			.andExpect(content().bytes(png.getBytes()));

		var invalid = new MockMultipartFile("avatar", "notes.txt", "text/plain", "not an image".getBytes());
		mockMvc.perform(multipart("/account/avatar").file(invalid).with(csrf())
				.session((org.springframework.mock.web.MockHttpSession) session))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/account"));

		mockMvc.perform(get("/account/avatar").session((org.springframework.mock.web.MockHttpSession) session))
			.andExpect(status().isOk())
			.andExpect(content().contentType("image/png"))
			.andExpect(content().bytes(png.getBytes()));

		mockMvc.perform(get("/account").session((org.springframework.mock.web.MockHttpSession) session))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.not(
				org.hamcrest.Matchers.containsString("/account/avatar/remove"))))
			.andExpect(content().string(org.hamcrest.Matchers.not(
				org.hamcrest.Matchers.containsString("Remove profile photo"))));

		mockMvc.perform(post("/account/avatar/remove").with(csrf())
				.session((org.springframework.mock.web.MockHttpSession) session))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/account?photoRemoved"));

		mockMvc.perform(get("/account/avatar").session((org.springframework.mock.web.MockHttpSession) session))
			.andExpect(status().isNotFound());
	}

	@Test
	void castSignsInToTheCastDashboard() throws Exception {
		userAccountRepository.saveAndFlush(new UserAccount(
			"cast-sign-in@castfan.test",
			passwordEncoder.encode("secure-cast-pass-123"),
			"Mina Cast",
			UserRole.CAST,
			UserStatus.ACTIVE
		));

		HttpSession session = mockMvc.perform(post("/login")
				.with(csrf())
				.param("email", "cast-sign-in@castfan.test")
				.param("password", "secure-cast-pass-123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/cast"))
			.andReturn()
			.getRequest()
			.getSession(false);

		assertThat(session).isNotNull();
		mockMvc.perform(get("/account").session((org.springframework.mock.web.MockHttpSession) session))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/cast/profile"));
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
			.andExpect(redirectedUrl("/account/stores"))
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
