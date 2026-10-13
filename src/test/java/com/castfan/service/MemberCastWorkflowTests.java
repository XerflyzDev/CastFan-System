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

import com.castfan.entity.CastProfile;
import com.castfan.entity.MemberProfile;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastFollowRepository;
import com.castfan.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MemberCastWorkflowTests {

	@Autowired
	private MemberCastService memberCastService;

	@Autowired
	private ProfileService profileService;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private CastFollowRepository castFollowRepository;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void discoveryReturnsOnlyVisibleCastAndMarksFollowedProfiles() {
		var member = createMember("discover-member@castfan.test");
		var visible = createCast("visible-cast@castfan.test", "Visible Cast", true);
		createCast("hidden-cast@castfan.test", "Hidden Cast", false);

		var initialPage = memberCastService.discover(member.getUserAccount().getEmail(), PageRequest.of(0, 12));
		assertThat(initialPage.getContent()).extracting(view -> view.stageName()).containsExactly("Visible Cast");
		assertThat(initialPage.getContent().getFirst().following()).isFalse();

		memberCastService.follow(member.getUserAccount().getEmail(), visible.getId());
		var followedPage = memberCastService.discover(member.getUserAccount().getEmail(), PageRequest.of(0, 12));
		assertThat(followedPage.getContent().getFirst().following()).isTrue();
	}

	@Test
	void followIsIdempotentAndUnfollowAffectsOnlyTheCurrentMember() {
		var firstMember = createMember("first-follow-member@castfan.test");
		var secondMember = createMember("second-follow-member@castfan.test");
		var cast = createCast("followed-cast@castfan.test", "Followed Cast", true);

		memberCastService.follow(firstMember.getUserAccount().getEmail(), cast.getId());
		memberCastService.follow(firstMember.getUserAccount().getEmail(), cast.getId());
		memberCastService.follow(secondMember.getUserAccount().getEmail(), cast.getId());
		assertThat(castFollowRepository.count()).isEqualTo(2);

		memberCastService.unfollow(firstMember.getUserAccount().getEmail(), cast.getId());
		assertThat(memberCastService.following(firstMember.getUserAccount().getEmail())).isEmpty();
		assertThat(memberCastService.following(secondMember.getUserAccount().getEmail()))
			.extracting(view -> view.stageName())
			.containsExactly("Followed Cast");
	}

	@Test
	void nonMemberAndUnavailableCastAreRejected() {
		var member = createMember("restricted-member@castfan.test");
		var castAccount = saveAccount("actor-cast@castfan.test", UserRole.CAST);
		var hiddenCast = createCast("private-cast@castfan.test", "Private Cast", false);

		assertThatThrownBy(() -> memberCastService.follow(member.getUserAccount().getEmail(), hiddenCast.getId()))
			.isInstanceOf(DomainRuleViolationException.class);
		assertThatThrownBy(() -> memberCastService.discover(castAccount.getEmail(), PageRequest.of(0, 12)))
			.isInstanceOf(DomainRuleViolationException.class);
	}

	@Test
	void memberEndpointsEnforceRoleAndCsrf() throws Exception {
		var member = createMember("web-member@castfan.test");
		var cast = createCast("web-cast@castfan.test", "Web Cast", true);
		var memberRequest = user(member.getUserAccount().getEmail()).roles("MEMBER");

		mockMvc.perform(get("/member/casts"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));
		mockMvc.perform(get("/member/casts").with(user("cast@castfan.test").roles("CAST")))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/member/casts").with(memberRequest))
			.andExpect(status().isOk())
			.andExpect(view().name("member/casts"));

		mockMvc.perform(post("/member/casts/{id}/follow", cast.getId()).with(memberRequest))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/member/casts/{id}/follow", cast.getId()).with(memberRequest).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/member/casts"));

		assertThat(castFollowRepository.existsByMemberProfileIdAndCastProfileId(member.getId(), cast.getId())).isTrue();
	}

	private MemberProfile createMember(String email) {
		return profileService.createMemberProfile(saveAccount(email, UserRole.MEMBER));
	}

	private CastProfile createCast(String email, String stageName, boolean discoverable) {
		return profileService.createCastProfile(
			saveAccount(email, UserRole.CAST),
			stageName,
			stageName + " profile",
			null,
			discoverable
		);
	}

	private UserAccount saveAccount(String email, UserRole role) {
		return userAccountRepository.saveAndFlush(
			new UserAccount(email, "encoded-password", email, role, UserStatus.ACTIVE)
		);
	}
}
