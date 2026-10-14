package com.castfan.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
	"spring.profiles.active=test",
	"castfan.security.development-endpoints=false",
	"castfan.security.require-https=false"
})
@AutoConfigureMockMvc
class ProductionHealthTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthEndpointIsPublicAndDoesNotExposeComponents() throws Exception {
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"))
			.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void livenessEndpointIsPublicAndDoesNotExposeComponents() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"))
			.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void nonHealthActuatorEndpointsAreDenied() throws Exception {
		var authenticatedMember = user("member@castfan.test").roles("MEMBER");

		mockMvc.perform(get("/actuator").with(authenticatedMember))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/actuator/env").with(authenticatedMember))
			.andExpect(status().isForbidden());
	}
}
