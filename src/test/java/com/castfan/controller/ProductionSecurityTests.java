package com.castfan.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class ProductionSecurityTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void productionResponsesIncludeRestrictiveBrowserHeaders() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(header().string("X-Content-Type-Options", "nosniff"))
			.andExpect(header().string("X-Frame-Options", "DENY"))
			.andExpect(header().string("Referrer-Policy", "same-origin"))
			.andExpect(header().string("Permissions-Policy", "camera=(), microphone=(), geolocation=()"))
			.andExpect(header().string("Content-Security-Policy",
				org.hamcrest.Matchers.containsString("frame-ancestors 'none'")));
	}

	@Test
	void developmentOnlyResourcesAreDeniedInProductionMode() throws Exception {
		var authenticatedMember = user("member@castfan.test").roles("MEMBER");

		mockMvc.perform(get("/concepts/member-home.html").with(authenticatedMember))
			.andExpect(status().isForbidden());
		mockMvc.perform(get("/h2-console/").with(authenticatedMember))
			.andExpect(status().isForbidden());
	}
}
