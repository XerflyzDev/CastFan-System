package com.castfan.service;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.Instant;

import com.castfan.entity.CastContent;
import com.castfan.entity.CastFollow;
import com.castfan.entity.CastProfile;
import com.castfan.entity.ContentAudience;
import com.castfan.entity.EventStatus;
import com.castfan.entity.MemberProfile;
import com.castfan.entity.MemberTier;
import com.castfan.entity.Store;
import com.castfan.entity.StoreEvent;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastContentRepository;
import com.castfan.repository.CastFollowRepository;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.MemberProfileRepository;
import com.castfan.repository.MemberTierRepository;
import com.castfan.repository.StoreEventRepository;
import com.castfan.repository.StoreRepository;
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
class MemberNotificationWorkflowTests {

	@Autowired private MockMvc mockMvc;
	@Autowired private UserAccountRepository userAccountRepository;
	@Autowired private MemberProfileRepository memberProfileRepository;
	@Autowired private CastProfileRepository castProfileRepository;
	@Autowired private CastFollowRepository castFollowRepository;
	@Autowired private MemberTierRepository memberTierRepository;
	@Autowired private CastContentRepository castContentRepository;
	@Autowired private StoreRepository storeRepository;
	@Autowired private StoreEventRepository storeEventRepository;

	@Test
	void notificationCenterShowsFollowedUpdatesAndPublishedEventsWithoutLeakingLockedBody() throws Exception {
		var suffix = Long.toString(System.nanoTime());
		var memberAccount = account("notice-member-" + suffix + "@test.local", "Notice Member", UserRole.MEMBER);
		var member = memberProfileRepository.save(new MemberProfile(memberAccount, null, null));
		var followedCast = cast("notice-cast-" + suffix + "@test.local", "Notice Mina " + suffix);
		var otherCast = cast("other-notice-cast-" + suffix + "@test.local", "Other Notice " + suffix);
		castFollowRepository.save(new CastFollow(member, followedCast));
		var tier = memberTierRepository.save(new MemberTier(
			followedCast, "Gold", "gold", "Gold access", 1, new BigDecimal("399.00"), "THB"
		));
		publish(followedCast, "Locked member update", "Locked notification secret", ContentAudience.MEMBERS, tier);
		publish(otherCast, "Unfollowed update", "Must not be listed", ContentAudience.PUBLIC, null);

		var owner = account("notice-owner-" + suffix + "@test.local", "Notice Owner", UserRole.OWNER);
		var store = storeRepository.save(new Store(owner, "Notice Cafe", "notice-cafe-" + suffix,
			null, null, "Asia/Bangkok", true));
		var startsAt = Instant.now().plusSeconds(86_400);
		storeEventRepository.save(new StoreEvent(
			store, owner, "Published Notice Event", "Upcoming event", null,
			startsAt, startsAt.plusSeconds(7_200), EventStatus.PUBLISHED
		));
		storeEventRepository.save(new StoreEvent(
			store, owner, "Draft Notice Event", "Draft event", null,
			startsAt, startsAt.plusSeconds(7_200), EventStatus.DRAFT
		));

		mockMvc.perform(get("/member/notifications").with(user(memberAccount.getEmail()).roles("MEMBER")))
			.andExpect(status().isOk()).andExpect(view().name("member/notifications"))
			.andExpect(content().string(containsString("Locked member update")))
			.andExpect(content().string(containsString("Gold+ members")))
			.andExpect(content().string(not(containsString("Locked notification secret"))))
			.andExpect(content().string(not(containsString("Unfollowed update"))))
			.andExpect(content().string(containsString("Published Notice Event")))
			.andExpect(content().string(not(containsString("Draft Notice Event"))));

		mockMvc.perform(post("/member/profile").with(user(memberAccount.getEmail()).roles("MEMBER")).with(csrf())
				.param("displayName", "Notice Member").param("bio", "").param("avatarUrl", ""))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/member/profile?saved"));
		mockMvc.perform(get("/member/notifications").with(user(memberAccount.getEmail()).roles("MEMBER")))
			.andExpect(status().isOk())
			.andExpect(content().string(not(containsString("Locked member update"))))
			.andExpect(content().string(not(containsString("Published Notice Event"))))
			.andExpect(content().string(containsString("No Cast updates yet")))
			.andExpect(content().string(containsString("No upcoming events")));
	}

	@Test
	void notificationCenterRequiresMemberAuthentication() throws Exception {
		var cast = account("notice-role-cast-" + System.nanoTime() + "@test.local", "Cast", UserRole.CAST);
		mockMvc.perform(get("/member/notifications"))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
		mockMvc.perform(get("/member/notifications").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isForbidden());
	}

	private UserAccount account(String email, String name, UserRole role) {
		return userAccountRepository.save(new UserAccount(email, "encoded", name, role, UserStatus.ACTIVE));
	}

	private CastProfile cast(String email, String stageName) {
		return castProfileRepository.save(new CastProfile(
			account(email, stageName, UserRole.CAST), stageName, stageName.toLowerCase().replace(' ', '-'),
			null, null, true
		));
	}

	private void publish(CastProfile cast, String title, String body, ContentAudience audience, MemberTier tier) {
		var content = new CastContent(cast, title, body, null, audience, tier);
		content.publish();
		castContentRepository.save(content);
	}
}
