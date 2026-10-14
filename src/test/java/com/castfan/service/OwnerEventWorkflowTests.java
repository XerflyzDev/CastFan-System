package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.ZoneId;

import com.castfan.entity.EventStatus;
import com.castfan.entity.Store;
import com.castfan.entity.UserAccount;
import com.castfan.repository.StoreEventRepository;
import org.junit.jupiter.api.BeforeEach;
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
class OwnerEventWorkflowTests {

	private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Bangkok");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PrivilegedAccountProvisioningService provisioningService;

	@Autowired
	private StoreService storeService;

	@Autowired
	private StoreEventRepository storeEventRepository;

	private UserAccount owner;
	private Store store;
	private LocalDate eventDate;

	@BeforeEach
	void setUp() {
		var suffix = Long.toString(System.nanoTime());
		owner = provisioningService.provisionOwner(
			"event-owner-" + suffix + "@castfan.test", "secure-owner-pass-123", "Event Owner"
		);
		store = storeService.createStore(
			owner, "Event Test Cafe", "event-test-" + suffix, null, "Bangkok", STORE_ZONE.getId()
		);
		eventDate = LocalDate.now(STORE_ZONE).plusDays(14);
	}

	@Test
	void ownerCanOpenEventWorkspaceAndCreateDraft() throws Exception {
		mockMvc.perform(get("/owner/events").with(ownerUser()))
			.andExpect(status().isOk())
			.andExpect(view().name("owner/events"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Event management")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Event Test Cafe")));

		createEvent("Draft Launch")
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/events?created"));

		var event = storeEventRepository.findAllForOwner(owner.getEmail()).getFirst();
		assertThat(event.getTitle()).isEqualTo("Draft Launch");
		assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
	}

	@Test
	void ownerCanEditPublishAndArchiveAnEvent() throws Exception {
		createEvent("First Title").andExpect(status().is3xxRedirection());
		var event = storeEventRepository.findAllForOwner(owner.getEmail()).getFirst();

		mockMvc.perform(post("/owner/events/{id}", event.getId())
				.with(ownerUser()).with(csrf())
				.param("storeId", store.getId().toString())
				.param("title", "Published Night")
				.param("description", "Updated event details")
				.param("imageUrl", "/images/event-autumn.svg")
				.param("date", eventDate.toString())
				.param("startsAt", "17:00")
				.param("endsAt", "22:00"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/events?saved"));

		mockMvc.perform(post("/owner/events/{id}/publish", event.getId())
				.with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/events?published=true"));

		mockMvc.perform(get("/events/{id}", event.getId()))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Published Night")));

		mockMvc.perform(post("/owner/events/{id}/archive", event.getId())
				.with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/events?archived=true"));

		assertThat(storeEventRepository.findById(event.getId()).orElseThrow().getStatus())
			.isEqualTo(EventStatus.ARCHIVED);
		mockMvc.perform(get("/events/{id}", event.getId())).andExpect(status().isNotFound());
	}

	@Test
	void anotherOwnerCannotReadOrChangeTheEvent() throws Exception {
		createEvent("Private Draft").andExpect(status().is3xxRedirection());
		var event = storeEventRepository.findAllForOwner(owner.getEmail()).getFirst();
		var otherOwner = provisioningService.provisionOwner(
			"other-event-owner-" + System.nanoTime() + "@castfan.test", "secure-owner-pass-123", "Other Owner"
		);

		mockMvc.perform(get("/owner/events/{id}/edit", event.getId())
				.with(user(otherOwner.getEmail()).roles("OWNER")))
			.andExpect(status().isNotFound());

		mockMvc.perform(post("/owner/events/{id}/publish", event.getId())
				.with(user(otherOwner.getEmail()).roles("OWNER")).with(csrf()))
			.andExpect(status().is3xxRedirection());

		assertThat(storeEventRepository.findById(event.getId()).orElseThrow().getStatus())
			.isEqualTo(EventStatus.DRAFT);
	}

	@Test
	void pastEventCannotBePublished() throws Exception {
		mockMvc.perform(post("/owner/events")
				.with(ownerUser()).with(csrf())
				.param("storeId", store.getId().toString())
				.param("title", "Past Event")
				.param("description", "A historical event")
				.param("imageUrl", "")
				.param("date", LocalDate.now(STORE_ZONE).minusDays(2).toString())
				.param("startsAt", "10:00")
				.param("endsAt", "12:00"))
			.andExpect(status().is3xxRedirection());
		var event = storeEventRepository.findAllForOwner(owner.getEmail()).getFirst();

		mockMvc.perform(post("/owner/events/{id}/publish", event.getId())
				.with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection());

		assertThat(storeEventRepository.findById(event.getId()).orElseThrow().getStatus())
			.isEqualTo(EventStatus.DRAFT);
	}

	private org.springframework.test.web.servlet.ResultActions createEvent(String title) throws Exception {
		return mockMvc.perform(post("/owner/events")
			.with(ownerUser()).with(csrf())
			.param("storeId", store.getId().toString())
			.param("title", title)
			.param("description", "A members-only store event")
			.param("imageUrl", "")
			.param("date", eventDate.toString())
			.param("startsAt", "16:00")
			.param("endsAt", "21:00"));
	}

	private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor ownerUser() {
		return user(owner.getEmail()).roles("OWNER");
	}
}
