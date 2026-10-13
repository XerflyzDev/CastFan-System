package com.castfan.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
class HomeControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void homePageIsPublicAndRendersTheMainView() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(view().name("index"))
			.andExpect(model().attribute("pageTitle", "CastFan"))
			.andExpect(model().attributeExists("home"))
			.andExpect(content().string(containsString("Cast Working Today")));
	}

	@Test
	void applicationStylesheetIsPublic() throws Exception {
		mockMvc.perform(get("/css/app.css"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/css"));

		mockMvc.perform(get("/fonts/line-seed-sans-th/LINESeedSansTH_W_Rg.woff2"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("font/woff2"));
	}

	@Test
	void applicationJavascriptAndEventImageArePublic() throws Exception {
		mockMvc.perform(get("/js/app.js"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/javascript"));

		mockMvc.perform(get("/images/autumn-cafe-event.png"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("image/png"));
	}
}
