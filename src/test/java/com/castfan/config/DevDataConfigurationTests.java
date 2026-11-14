package com.castfan.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.castfan.entity.UserRole;
import com.castfan.repository.AccountAvatarRepository;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberPointTransactionRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.StoreMembershipRepository;
import com.castfan.repository.StorePromotionRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DevDataConfigurationTests {

	@Autowired UserAccountRepository accounts;
	@Autowired AccountAvatarRepository avatars;
	@Autowired MemberProfileRepository memberProfiles;
	@Autowired StoreRepository stores;
	@Autowired StoreMembershipRepository memberships;
	@Autowired CastProfileRepository castProfiles;
	@Autowired StoreCastRepository storeCasts;
	@Autowired ShiftScheduleRepository shifts;
	@Autowired MemberPointTransactionRepository pointTransactions;
	@Autowired StoreEventRepository events;
	@Autowired StorePromotionRepository promotions;
	@Autowired MockMvc mockMvc;

	@Test
	void seedsOnlyMinimumRoleAccountsWithoutImagesOrSampleActivity() {
		assertThat(accounts.count()).isEqualTo(3);
		assertThat(stores.count()).isEqualTo(1);
		assertThat(memberProfiles.count()).isEqualTo(2);
		assertThat(memberships.count()).isEqualTo(1);
		assertThat(castProfiles.count()).isEqualTo(1);
		assertThat(storeCasts.count()).isEqualTo(1);

		assertThat(accounts.findByEmailIgnoreCase("owner@castfan.local"))
			.hasValueSatisfying(account -> assertThat(account.getRole()).isEqualTo(UserRole.OWNER));
		assertThat(accounts.findByEmailIgnoreCase("member@castfan.local"))
			.hasValueSatisfying(account -> assertThat(account.getRole()).isEqualTo(UserRole.MEMBER));
		assertThat(accounts.findByEmailIgnoreCase("mina@castfan.local"))
			.hasValueSatisfying(account -> assertThat(account.getRole()).isEqualTo(UserRole.CAST));
		assertThat(accounts.findByEmailIgnoreCase("yuki@castfan.local")).isEmpty();
		assertThat(accounts.findByEmailIgnoreCase("ami@castfan.local")).isEmpty();

		assertThat(memberProfiles.findAll())
			.allSatisfy(profile -> {
				assertThat(profile.getBio()).isNull();
				assertThat(profile.getAvatarUrl()).isNull();
			});
		assertThat(castProfiles.findByUserAccountLoginIdentifierIgnoreCase("mina@castfan.local"))
			.hasValueSatisfying(profile -> {
				assertThat(profile.getBio()).isNull();
				assertThat(profile.getAvatarUrl()).isNull();
			});
		assertThat(memberships.findAll())
			.singleElement()
			.satisfies(membership -> assertThat(membership.getPointsBalance()).isZero());

		assertThat(avatars.count()).isZero();
		assertThat(shifts.count()).isZero();
		assertThat(pointTransactions.count()).isZero();
		assertThat(events.count()).isZero();
		assertThat(promotions.count()).isZero();
	}

	@Test
	void minimumAccountsCanSignInAndOpenTheirRoleLandingPages() throws Exception {
		assertRoleLandingPage("owner@castfan.local", "/owner");
		assertRoleLandingPage("member@castfan.local", "/s/castfan/home");
		assertRoleLandingPage("mina@castfan.local", "/cast");
	}

	private void assertRoleLandingPage(String login, String path) throws Exception {
		var result = mockMvc.perform(formLogin().userParameter("email").user(login).password("dev-only-password"))
			.andExpect(authenticated().withUsername(login))
			.andReturn();
		var session = (MockHttpSession) result.getRequest().getSession(false);
		assertThat(session).isNotNull();
		mockMvc.perform(get(path).session(session))
			.andExpect(status().isOk());
	}
}
