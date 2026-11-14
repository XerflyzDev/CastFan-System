package com.castfan.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
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
	void anonymousHomePageRendersTheWelcomeExperience() throws Exception {
		mockMvc.perform(get("/"))
			.andExpect(status().isOk())
			.andExpect(view().name("welcome"))
			.andExpect(model().attributeDoesNotExist("home"))
			.andExpect(content().string(containsString("Welcome to CastFan")))
			.andExpect(content().string(containsString("Meet. Follow. Visit.")))
			.andExpect(content().string(containsString("href=\"/login\"")))
			.andExpect(content().string(containsString("Get Started")));
	}

	@Test
	void ownerHomeShowsDashboardNavigation() throws Exception {
		mockMvc.perform(get("/").with(user("owner@castfan.test").roles("OWNER")))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Owner Dashboard")))
			.andExpect(content().string(containsString("href=\"/owner/shifts\"")))
			.andExpect(content().string(containsString("href=\"/owner/events\"")))
			.andExpect(content().string(containsString("href=\"/owner/promotions\"")))
			.andExpect(content().string(containsString("href=\"/owner\"")))
			.andExpect(content().string(containsString("href=\"/account\"")))
			.andExpect(content().string(matchesPattern(
				"(?s).*<nav class=\"floating-nav owner-nav\" aria-label=\"Owner navigation\">"
					+ ".*?<span>Dashboard</span>.*?<span>Shifts</span>.*?<span>Events</span>"
					+ ".*?<span>Promos</span>.*?<span>Account</span>.*?</nav>.*"
			)));
	}

	@Test
	void castHomeShowsMemberNavigationWithCastWorkspace() throws Exception {
		mockMvc.perform(get("/").with(user("cast@castfan.test").roles("CAST")))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("href=\"/cast/schedule\"")))
			.andExpect(content().string(containsString("href=\"/member/notifications\"")))
			.andExpect(content().string(containsString("href=\"/cast/public-profile\"")))
			.andExpect(content().string(containsString("href=\"/cast/profile\"")))
			.andExpect(content().string(containsString("href=\"/cast\"")))
			.andExpect(content().string(containsString("aria-label=\"Cast Profile\"")))
			.andExpect(content().string(containsString("aria-label=\"Profile\"")))
			.andExpect(content().string(containsString("aria-label=\"Cast\"")))
			.andExpect(content().string(matchesPattern(
				"(?s).*<nav class=\"floating-nav cast-nav\" aria-label=\"Cast navigation\">"
					+ ".*?<span>Home</span>.*?<span>Schedule</span>.*?<span>Cast</span>"
					+ ".*?<span>Cast Profile</span>.*?<span>Profile</span>.*?</nav>.*"
			)));
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

		mockMvc.perform(get("/js/auth.js"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/javascript"));

		mockMvc.perform(get("/js/member-home.js"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/javascript"));

		mockMvc.perform(get("/js/member-following.js"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/javascript"));

		mockMvc.perform(get("/js/member-profile.js"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/javascript"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("reader.readAsDataURL(file)")))
			.andExpect(content().string(org.hamcrest.Matchers.not(
				org.hamcrest.Matchers.containsString("URL.createObjectURL(file)"))));

		mockMvc.perform(get("/js/public-detail.js"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/javascript"));

		mockMvc.perform(get("/js/cast-workspace.js"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/javascript"));

		mockMvc.perform(get("/js/owner-workspace.js"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/javascript"));

		mockMvc.perform(get("/js/confirmation-dialog.js"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/javascript"));

		mockMvc.perform(get("/css/cast-profile.css"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/css"));

		mockMvc.perform(get("/css/event-detail.css"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/css"));

		mockMvc.perform(get("/css/member-following.css"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/css"));

		mockMvc.perform(get("/css/member-profile.css"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/css"));

		mockMvc.perform(get("/css/cast-workspace.css"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/css"));

		mockMvc.perform(get("/css/owner-workspace.css"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/css"));

		mockMvc.perform(get("/images/autumn-cafe-event.png"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("image/png"));
	}

	@Test
	void memberHomeConceptIsPublic() throws Exception {
		mockMvc.perform(get("/concepts/member-home.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/html"))
			.andExpect(content().string(containsString("Good morning")));

		mockMvc.perform(get("/concepts/member-schedule.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/html"))
			.andExpect(content().string(containsString("Today's Shifts")));

		mockMvc.perform(get("/concepts/member-following.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/html"))
			.andExpect(content().string(containsString("Search followed Cast")));

		mockMvc.perform(get("/concepts/member-profile.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/html"))
			.andExpect(content().string(containsString("Personal information")));

		mockMvc.perform(get("/concepts/cast-profile.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/html"))
			.andExpect(content().string(containsString("Latest Update")));

		mockMvc.perform(get("/concepts/event-detail.html"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/html"))
			.andExpect(content().string(containsString("Add to Calendar")));
	}
}
