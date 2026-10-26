package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;

import com.castfan.dto.CastContentForm;
import com.castfan.dto.MemberTierForm;
import com.castfan.entity.ContentAudience;
import com.castfan.entity.ContentStatus;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastContentRepository;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CastContentWorkflowTests {
	@Autowired private MockMvc mockMvc;
	@Autowired private UserAccountRepository userAccountRepository;
	@Autowired private CastProfileRepository castProfileRepository;
	@Autowired private CastContentRepository castContentRepository;
	@Autowired private ProfileService profileService;
	@Autowired private CastContentService castContentService;
	@Autowired private MembershipService membershipService;
	@Autowired private MemberCastService memberCastService;

	private UserAccount castAccount;
	private Long castProfileId;

	@BeforeEach
	void setUp() {
		var suffix = Long.toString(System.nanoTime());
		castAccount = saveAccount("content-cast-" + suffix + "@castfan.test", UserRole.CAST);
		castProfileId = profileService.createCastProfile(castAccount, "Content Cast " + suffix, null, null, true).getId();
	}

	@Test
	void castCanCreateEditPublishAndArchiveContent() throws Exception {
		mockMvc.perform(get("/cast/content").with(castUser()))
			.andExpect(status().isOk()).andExpect(view().name("cast/content"))
			.andExpect(content().string(containsString("Share with your fans")))
			.andExpect(content().string(containsString("Post now")));

		var image = png("content.png");
		mockMvc.perform(multipart("/cast/content").file(image).with(castUser()).with(csrf())
				.param("title", "First Update").param("body", "Public content body")
				.param("audience", "PUBLIC").param("minimumTierId", "").param("intent", "draft"))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cast/content?created"));
		var post = castContentRepository.findAllOwned(castAccount.getEmail()).getFirst();
		assertThat(post.getStatus()).isEqualTo(ContentStatus.DRAFT);

		mockMvc.perform(post("/cast/content/{id}", post.getId()).with(castUser()).with(csrf())
				.param("title", "Published Update").param("body", "Public content body")
				.param("audience", "PUBLIC").param("minimumTierId", ""))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cast/content?saved"));
		mockMvc.perform(post("/cast/content/{id}/publish", post.getId()).with(castUser()).with(csrf()))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cast/content?published=true"));
		mockMvc.perform(get("/casts/{id}", castProfileId))
			.andExpect(status().isOk()).andExpect(content().string(containsString("Public content body")));
		mockMvc.perform(get("/casts/{castId}/content/{contentId}", castProfileId, post.getId()).with(castUser()))
			.andExpect(status().isOk()).andExpect(view().name("public/content-detail"))
			.andExpect(content().string(containsString("Public content body")))
			.andExpect(content().string(matchesPattern(
				"(?s).*href=\"/cast/public-profile\" aria-label=\"Cast Profile\".*?aria-current=\"page\".*"
			)));
		mockMvc.perform(get("/casts/{castId}/content/{contentId}/image", castProfileId, post.getId()))
			.andExpect(status().isOk()).andExpect(content().contentType("image/png"));

		mockMvc.perform(post("/cast/content/{id}/archive", post.getId()).with(castUser()).with(csrf()))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cast/content?archived=true"));
		mockMvc.perform(get("/casts/{id}", castProfileId))
			.andExpect(content().string(not(containsString("Public content body"))));
		mockMvc.perform(get("/casts/{castId}/content/{contentId}", castProfileId, post.getId()))
			.andExpect(status().isNotFound());
	}

	@Test
	void castCanPublishSimplePostWithoutHeadline() throws Exception {
		mockMvc.perform(post("/cast/content").with(castUser()).with(csrf())
				.param("title", "").param("body", "A quick update for everyone")
				.param("audience", "PUBLIC").param("minimumTierId", "").param("intent", "publish"))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/cast/content?published"));

		var post = castContentRepository.findAllOwned(castAccount.getEmail()).getFirst();
		assertThat(post.getTitle()).isEqualTo("A quick update for everyone");
		assertThat(post.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
	}

	@Test
	void followerAndMemberContentIsLockedUntilViewerQualifies() throws Exception {
		var followerPost = createAndPublish("Follower Note", "Follower secret", ContentAudience.FOLLOWERS, null);
		var tier = membershipService.createTier(castAccount.getEmail(), tierForm("Gold", 2));
		var goldPost = createAndPublish("Gold Note", "Gold secret", ContentAudience.MEMBERS, tier.id());
		var member = saveAccount("content-member-" + System.nanoTime() + "@castfan.test", UserRole.MEMBER);
		profileService.createMemberProfile(member);

		mockMvc.perform(get("/casts/{id}", castProfileId))
			.andExpect(content().string(containsString("Follower Note")))
			.andExpect(content().string(not(containsString("Follower secret"))))
			.andExpect(content().string(not(containsString("Gold secret"))));
		mockMvc.perform(get("/casts/{castId}/content/{contentId}", castProfileId, followerPost.getId()))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("This update is locked")))
			.andExpect(content().string(not(containsString("Follower secret"))));

		memberCastService.follow(member.getEmail(), castProfileId);
		mockMvc.perform(get("/casts/{id}", castProfileId).with(user(member.getEmail()).roles("MEMBER")))
			.andExpect(content().string(containsString("Follower secret")))
			.andExpect(content().string(not(containsString("Gold secret"))));
		mockMvc.perform(get("/casts/{castId}/content/{contentId}", castProfileId, followerPost.getId())
				.with(user(member.getEmail()).roles("MEMBER")))
			.andExpect(status().isOk()).andExpect(content().string(containsString("Follower secret")));

		var castViewer = saveAccount("content-cast-viewer-" + System.nanoTime() + "@castfan.test", UserRole.CAST);
		profileService.createMemberProfile(castViewer);
		profileService.createCastProfile(
			castViewer, "Content Viewer " + System.nanoTime(), null, null, true
		);
		memberCastService.follow(castViewer.getEmail(), castProfileId);
		mockMvc.perform(get("/casts/{castId}/content/{contentId}", castProfileId, followerPost.getId())
				.with(user(castViewer.getEmail()).roles("CAST")))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Follower secret")))
			.andExpect(content().string(containsString("href=\"/cast\"")));

		mockMvc.perform(get("/casts/{castId}/content/{contentId}", castProfileId, goldPost.getId())
				.with(user(member.getEmail()).roles("MEMBER")))
			.andExpect(content().string(containsString("This update is locked")))
			.andExpect(content().string(not(containsString("Gold secret"))));

		membershipService.subscribe(member.getEmail(), tier.id());
		mockMvc.perform(get("/casts/{id}", castProfileId).with(user(member.getEmail()).roles("MEMBER")))
			.andExpect(content().string(containsString("Gold secret")));
		mockMvc.perform(get("/casts/{castId}/content/{contentId}", castProfileId, goldPost.getId())
				.with(user(member.getEmail()).roles("MEMBER")))
			.andExpect(content().string(containsString("Gold secret")));
		assertThat(followerPost.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
	}

	@Test
	void contentDetailDoesNotAllowCrossProfileIdLookup() throws Exception {
		var post = createAndPublish("Private Route", "Route body", ContentAudience.PUBLIC, null);
		var other = saveAccount("detail-other-cast-" + System.nanoTime() + "@castfan.test", UserRole.CAST);
		var otherProfileId = profileService.createCastProfile(
			other, "Detail Other " + System.nanoTime(), null, null, true
		).getId();

		mockMvc.perform(get("/casts/{castId}/content/{contentId}", otherProfileId, post.getId()))
			.andExpect(status().isNotFound())
			.andExpect(content().string(not(containsString("Route body"))));
	}

	@Test
	void anotherCastCannotChangeContentAndCreationRequiresCsrf() throws Exception {
		var post = createAndPublish("Owned Post", "Owner-only edit", ContentAudience.PUBLIC, null);
		var other = saveAccount("other-content-cast-" + System.nanoTime() + "@castfan.test", UserRole.CAST);
		profileService.createCastProfile(other, "Other Content Cast " + System.nanoTime(), null, null, true);

		mockMvc.perform(post("/cast/content").with(castUser())
				.param("title", "No CSRF").param("body", "Rejected").param("audience", "PUBLIC"))
			.andExpect(status().isForbidden());
		mockMvc.perform(post("/cast/content/{id}/archive", post.getId())
				.with(user(other.getEmail()).roles("CAST")).with(csrf()))
			.andExpect(status().is3xxRedirection());
		assertThat(castContentRepository.findById(post.getId()).orElseThrow().getStatus())
			.isEqualTo(ContentStatus.PUBLISHED);
	}

	private com.castfan.entity.CastContent createAndPublish(String title, String body, ContentAudience audience, Long tierId) {
		var form = new CastContentForm();
		form.setTitle(title); form.setBody(body); form.setAudience(audience); form.setMinimumTierId(tierId);
		castContentService.create(castAccount.getEmail(), form);
		var post = castContentRepository.findAllOwned(castAccount.getEmail()).getFirst();
		castContentService.publish(castAccount.getEmail(), post.getId());
		return post;
	}

	private MemberTierForm tierForm(String name, int rank) {
		var form = new MemberTierForm();
		form.setName(name); form.setDescription(name + " access"); form.setRank(rank);
		form.setMonthlyPrice(new BigDecimal("399.00"));
		return form;
	}

	private UserAccount saveAccount(String email, UserRole role) {
		return userAccountRepository.saveAndFlush(new UserAccount(email, "encoded-password", email, role, UserStatus.ACTIVE));
	}

	private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor castUser() {
		return user(castAccount.getEmail()).roles("CAST");
	}

	private MockMultipartFile png(String name) {
		return new MockMultipartFile("image", name, "image/png",
			new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1});
	}
}
