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
		owner = provisioningService.provisionOwner("roster-owner-" + suffix + "@castfan.test",
			"secure-owner-pass-123", "CastFan Owner");
		cast = provisioningService.provisionCast("roster-cast-" + suffix + "@castfan.test",
			"secure-cast-pass-123", "Cast account", "Mina " + suffix);
		store = storeService.createStore(owner, "Internal system store", "system-" + suffix,
			null, "Bangkok", "Asia/Bangkok");
	}

	@Test
	void ownerSeesOnlyTheCastFanRosterWithoutStoreControls() throws Exception {
		mockMvc.perform(get("/owner/cast").with(ownerUser()))
			.andExpect(status().isOk()).andExpect(view().name("owner/stores"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Cast roster")))
			.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Choose a store"))))
			.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Create store"))));

		mockMvc.perform(get("/owner/stores").with(ownerUser()))
			.andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Cast roster")));

		mockMvc.perform(post("/owner/stores").with(ownerUser()).with(csrf()))
			.andExpect(status().isMethodNotAllowed());
	}

	@Test
	void ownerCanManageRosterWithoutSendingAStoreId() throws Exception {
		var profile = castProfileRepository.findByUserAccountId(cast.getId()).orElseThrow();
		mockMvc.perform(post("/owner/cast").with(ownerUser()).with(csrf())
				.param("castProfileId", profile.getId().toString()))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/owner/cast?assigned"));

		var assignment = storeCastRepository.findByStoreIdAndCastProfileId(store.getId(), profile.getId()).orElseThrow();
		mockMvc.perform(post("/owner/cast/{id}/deactivate", assignment.getId()).with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/owner/cast?removed=true"));
		assertThat(storeCastRepository.findById(assignment.getId()).orElseThrow().isActive()).isFalse();

		mockMvc.perform(post("/owner/cast/{id}/activate", assignment.getId()).with(ownerUser()).with(csrf()))
			.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/owner/cast?restored=true"));
		assertThat(storeCastRepository.findById(assignment.getId()).orElseThrow().isActive()).isTrue();
	}

	@Test
	void removedStoreMutationEndpointsCannotChangeTheSystemStore() throws Exception {
		mockMvc.perform(post("/owner/stores/{id}/deactivate", store.getId()).with(ownerUser()).with(csrf()))
			.andExpect(status().isNotFound());
		assertThat(storeRepository.findById(store.getId()).orElseThrow().isActive()).isTrue();
	}

	private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor ownerUser() {
		return user(owner.getEmail()).roles("OWNER");
	}
}
