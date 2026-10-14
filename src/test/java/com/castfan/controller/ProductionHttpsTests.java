package com.castfan.controller;

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
	"castfan.security.require-https=true"
})
@AutoConfigureMockMvc
class ProductionHttpsTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void insecureRequestsRedirectToHttps() throws Exception {
		mockMvc.perform(get("/").secure(false))
			.andExpect(status().is3xxRedirection())
			.andExpect(header().string("Location", "https://localhost/"));
	}

	@Test
	void secureResponsesIncludeHsts() throws Exception {
		mockMvc.perform(get("/").with(request -> {
			request.setScheme("https");
			request.setSecure(true);
			return request;
		}))
			.andExpect(status().isOk())
			.andExpect(header().string("Strict-Transport-Security",
				org.hamcrest.Matchers.containsString("max-age=")));
	}
}
