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

import com.castfan.entity.Store;
import com.castfan.entity.UserAccount;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreRepository;
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
class OwnerStoreWorkflowTests {

	@Autowired private MockMvc mockMvc;
	@Autowired private PrivilegedAccountProvisioningService provisioningService;
	@Autowired private StoreService storeService;
	@Autowired private StoreRepository storeRepository;
	@Autowired private StoreCastRepository storeCastRepository;
	@Autowired private CastProfileRepository castProfileRepository;

	private UserAccount owner;
	private UserAccount cast;
	private Store store;

	@BeforeEach
	void setUp() {
		var suffix = Long.toString(System.nanoTime());
		owner = provisioningService.provisionOwner(
			"store-owner-" + suffix + "@castfan.test", "secure-owner-pass-123", "Store Owner"
		);
		cast = provisioningService.provisionCast(
			"store-cast-" + suffix + "@castfan.test", "secure-cast-pass-123", "Mina Cast", "Mina Store " + suffix
		);
		store = storeService.createStore(
			owner, "Hoshi Store", "hoshi-store-" + suffix, "Original description", "Bangkok", "Asia/Bangkok"
		);
	}

	@Test
	void ownerCanOpenCreateAndEditStoreManagement() throws Exception {
		mockMvc.perform(get("/owner/stores").with(ownerUser()))
			.andExpect(status().isOk())
			.andExpect(view().name("owner/stores"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Store management")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Hoshi Store")));

		var slug = "second-store-" + System.nanoTime();
		mockMvc.perform(post("/owner/stores").with(ownerUser()).with(csrf())
				.param("name", "Second Store")
				.param("slug", slug)
				.param("description", "A second location")
				.param("address", "Chiang Mai")
				.param("timezone", "Asia/Bangkok"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/stores?created"));
		assertThat(storeRepository.findBySlug(slug)).isPresent();

		mockMvc.perform(post("/owner/stores/{id}", store.getId()).with(ownerUser()).with(csrf())
				.param("name", "Hoshi Flagship")
				.param("slug", store.getSlug())
				.param("description", "Updated description")
				.param("address", "Siam, Bangkok")
				.param("timezone", "Asia/Bangkok"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/stores?saved"));
		assertThat(storeRepository.findById(store.getId()).orElseThrow().getName()).isEqualTo("Hoshi Flagship");
	}

	@Test
	void ownerCanManagePermanentCastRosterWithoutDeletingHistory() throws Exception {
		var profile = castProfileRepository.findByUserAccountId(cast.getId()).orElseThrow();
		mockMvc.perform(post("/owner/stores/cast").with(ownerUser()).with(csrf())
				.param("storeId", store.getId().toString())
				.param("castProfileId", profile.getId().toString()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/stores?assigned"));

		var assignment = storeCastRepository.findByStoreIdAndCastProfileId(store.getId(), profile.getId()).orElseThrow();
		mockMvc.perform(post("/owner/stores/cast/{id}/deactivate", assignment.getId())
				.with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/stores?removed=true"));
		assertThat(storeCastRepository.findById(assignment.getId()).orElseThrow().isActive()).isFalse();

		mockMvc.perform(post("/owner/stores/cast/{id}/activate", assignment.getId())
				.with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/stores?restored=true"));
		assertThat(storeCastRepository.findById(assignment.getId()).orElseThrow().isActive()).isTrue();
	}

	@Test
	void ownerCanPauseAndReactivateStore() throws Exception {
		mockMvc.perform(post("/owner/stores/{id}/deactivate", store.getId()).with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/stores?deactivated=true"));
		assertThat(storeRepository.findById(store.getId()).orElseThrow().isActive()).isFalse();

		mockMvc.perform(post("/owner/stores/{id}/activate", store.getId()).with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/stores?activated=true"));
		assertThat(storeRepository.findById(store.getId()).orElseThrow().isActive()).isTrue();
	}

	@Test
	void anotherOwnerCannotEditStoreOrRoster() throws Exception {
		var otherOwner = provisioningService.provisionOwner(
			"other-store-owner-" + System.nanoTime() + "@castfan.test", "secure-owner-pass-123", "Other Owner"
		);
		var profile = castProfileRepository.findByUserAccountId(cast.getId()).orElseThrow();

		mockMvc.perform(get("/owner/stores/{id}/edit", store.getId())
				.with(user(otherOwner.getEmail()).roles("OWNER")))
			.andExpect(status().isNotFound());

		mockMvc.perform(post("/owner/stores/cast").with(user(otherOwner.getEmail()).roles("OWNER")).with(csrf())
				.param("storeId", store.getId().toString())
				.param("castProfileId", profile.getId().toString()))
			.andExpect(status().isOk());
		assertThat(storeCastRepository.findByStoreIdAndCastProfileId(store.getId(), profile.getId())).isEmpty();
	}

	private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor ownerUser() {
		return user(owner.getEmail()).roles("OWNER");
	}
}
