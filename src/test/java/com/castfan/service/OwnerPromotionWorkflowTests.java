package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.ZoneId;

import com.castfan.entity.Store;
import com.castfan.entity.UserAccount;
import com.castfan.repository.StorePromotionRepository;
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
class OwnerPromotionWorkflowTests {
	private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Bangkok");
	@Autowired private MockMvc mockMvc;
	@Autowired private PrivilegedAccountProvisioningService provisioningService;
	@Autowired private StoreService storeService;
	@Autowired private StorePromotionRepository promotionRepository;
	private UserAccount owner;
	private Store store;
	private LocalDate today;

	@BeforeEach
	void setUp() {
		var suffix = Long.toString(System.nanoTime());
		owner = provisioningService.provisionOwner("promotion-owner-" + suffix + "@castfan.test", "secure-owner-pass-123", "Promotion Owner");
		store = storeService.createStore(owner, "Promotion Test Cafe", "promotion-test-" + suffix, null, "Bangkok", STORE_ZONE.getId());
		today = LocalDate.now(STORE_ZONE);
	}

	@Test
	void ownerCanCreateEditActivateAndPausePromotion() throws Exception {
		mockMvc.perform(get("/owner/promotions").with(ownerUser()))
			.andExpect(status().isOk()).andExpect(view().name("owner/promotions"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Promotions &amp; banners")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("data-picker-date")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("data-picker-time")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("date-time-picker.js")));
		createPromotion("Welcome offer", "WELCOME10").andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/owner/promotions?created"));
		var promotion = promotionRepository.findAllForOwner(owner.getEmail()).getFirst();
		assertThat(promotion.isActive()).isFalse();
		assertThat(promotion.getCode()).isEqualTo("WELCOME10");

		mockMvc.perform(post("/owner/promotions/{id}", promotion.getId()).with(ownerUser()).with(csrf())
			.param("storeId", store.getId().toString()).param("title", "Welcome members")
			.param("description", "Updated campaign terms").param("code", "member15")
			.param("startsOn", today.minusDays(1).toString()).param("startsAt", "09:00")
			.param("endsOn", today.plusDays(10).toString()).param("endsAt", "23:00"))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/owner/promotions?saved"));
		mockMvc.perform(post("/owner/promotions/{id}/activate", promotion.getId()).with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/owner/promotions?activated=true"));
		assertThat(promotionRepository.findById(promotion.getId()).orElseThrow().isActive()).isTrue();
		mockMvc.perform(get("/")).andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome members")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("MEMBER15")));
		mockMvc.perform(post("/owner/promotions/{id}/deactivate", promotion.getId()).with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/owner/promotions?deactivated=true"));
		mockMvc.perform(get("/")).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Welcome members"))));
	}

	@Test
	void ownerCanUploadAndClearPromotionImage() throws Exception {
		var image = new MockMultipartFile("image", "promotion.png", "image/png",
			new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1});
		mockMvc.perform(multipart("/owner/promotions").file(image).with(ownerUser()).with(csrf())
			.param("storeId", store.getId().toString()).param("title", "Image offer")
			.param("description", "Promotion with uploaded art").param("code", "IMAGE10")
			.param("startsOn", today.minusDays(1).toString()).param("startsAt", "09:00")
			.param("endsOn", today.plusDays(10).toString()).param("endsAt", "23:00"))
			.andExpect(status().is3xxRedirection());
		var promotion = promotionRepository.findAllForOwner(owner.getEmail()).getFirst();
		mockMvc.perform(get("/owner/promotions/{id}/image", promotion.getId()).with(ownerUser()))
			.andExpect(status().isOk()).andExpect(content().contentType("image/png"));
		mockMvc.perform(post("/owner/promotions/{id}/activate", promotion.getId()).with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/promotions/{id}/image", promotion.getId()))
			.andExpect(status().isOk()).andExpect(content().contentType("image/png"));
		mockMvc.perform(post("/owner/promotions/{id}", promotion.getId()).with(ownerUser()).with(csrf())
			.param("storeId", store.getId().toString()).param("title", "Image offer")
			.param("description", "Promotion with uploaded art").param("code", "IMAGE10")
			.param("removeImage", "true")
			.param("startsOn", today.minusDays(1).toString()).param("startsAt", "09:00")
			.param("endsOn", today.plusDays(10).toString()).param("endsAt", "23:00"))
			.andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/owner/promotions/{id}/image", promotion.getId()).with(ownerUser()))
			.andExpect(status().isNotFound());
	}

	@Test
	void codesAreGloballyUniqueAndInvalidRangesAreRejected() throws Exception {
		createPromotion("First offer", "SAMECODE").andExpect(status().is3xxRedirection());
		createPromotion("Second offer", "samecode").andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Promotion code is already in use.")));
		mockMvc.perform(post("/owner/promotions").with(ownerUser()).with(csrf())
			.param("storeId", store.getId().toString()).param("title", "Bad range")
			.param("description", "Invalid dates").param("code", "")
			.param("startsOn", today.plusDays(3).toString()).param("startsAt", "12:00")
			.param("endsOn", today.plusDays(2).toString()).param("endsAt", "12:00"))
			.andExpect(status().isOk())
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Promotion end time must be after its start time.")));
	}

	@Test
	void anotherOwnerCannotReadOrChangePromotion() throws Exception {
		createPromotion("Private campaign", "PRIVATE10").andExpect(status().is3xxRedirection());
		var promotion = promotionRepository.findAllForOwner(owner.getEmail()).getFirst();
		var other = provisioningService.provisionOwner("other-promotion-" + System.nanoTime() + "@castfan.test", "secure-owner-pass-123", "Other Owner");
		mockMvc.perform(get("/owner/promotions/{id}/edit", promotion.getId()).with(user(other.getEmail()).roles("OWNER"))).andExpect(status().isNotFound());
		mockMvc.perform(post("/owner/promotions/{id}/activate", promotion.getId()).with(user(other.getEmail()).roles("OWNER")).with(csrf())).andExpect(status().is3xxRedirection());
		assertThat(promotionRepository.findById(promotion.getId()).orElseThrow().isActive()).isFalse();
	}

	private org.springframework.test.web.servlet.ResultActions createPromotion(String title, String code) throws Exception {
		return mockMvc.perform(post("/owner/promotions").with(ownerUser()).with(csrf())
			.param("storeId", store.getId().toString()).param("title", title)
			.param("description", "Promotion campaign details").param("code", code)
			.param("startsOn", today.minusDays(1).toString()).param("startsAt", "09:00")
			.param("endsOn", today.plusDays(10).toString()).param("endsAt", "23:00"));
	}

	private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor ownerUser() {
		return user(owner.getEmail()).roles("OWNER");
	}
}
