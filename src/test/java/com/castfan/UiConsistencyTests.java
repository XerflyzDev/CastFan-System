package com.castfan;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class UiConsistencyTests {

	@Test
	void dateTimePickerClosesWhenItsViewportPositionBecomesStale() throws IOException {
		assertThat(resource("static/js/date-time-picker.js"))
			.contains("document.addEventListener(\"scroll\"")
			.contains("activePicker.popover.contains(event.target)")
			.contains("window.addEventListener(\"resize\"")
			.contains("closeActivePicker()");
	}

	@Test
	void castNavigationKeepsTheApprovedLabelsAndOrder() throws IOException {
		var navigation = resource("templates/fragments/navigation.html");
		var castNavigation = navigation.substring(
			navigation.indexOf("th:fragment=\"cast(active)\""),
			navigation.indexOf("th:fragment=\"owner(active)\"")
		);

		assertThat(castNavigation)
			.containsSubsequence(
				"<span>Home</span>",
				"<span>Schedule</span>",
				"<span>Cast Profile</span>",
				"<span>Profile</span>",
				"<span>Cast</span>"
			)
			.contains("th:href=\"@{/member/schedule}\"")
			.contains("th:href=\"@{/cast/public-profile}\"")
			.contains("th:href=\"@{/cast/profile}\"")
			.contains("th:href=\"@{/cast}\"")
			.contains("aria-label=\"Cast Profile\"")
			.contains("aria-label=\"Profile\"")
			.contains("aria-label=\"Cast\"");
	}

	@Test
	void workflowPagesExposeAConsistentBackActionToTheirParent() throws IOException {
		var workflowParents = Map.ofEntries(
			Map.entry("templates/member/notifications.html", "th:href=\"@{/}\" aria-label=\"Back to Home\""),
			Map.entry("templates/member/profile.html", "th:href=\"@{/account}\" aria-label=\"Back to Profile\""),
			Map.entry("templates/member/casts.html", "th:href=\"@{/member/following}\" aria-label=\"Back to Following\""),
			Map.entry("templates/member/memberships.html", "th:href=\"@{/account}\" aria-label=\"Back to Profile\""),
			Map.entry("templates/cast/schedule.html", "th:href=\"@{/cast}\" aria-label=\"Back to Cast workspace\""),
			Map.entry("templates/cast/tiers.html", "th:href=\"@{/cast}\" aria-label=\"Back to Cast workspace\""),
			Map.entry("templates/cast/content.html", "th:href=\"@{/cast}\" aria-label=\"Back to Cast workspace\""),
			Map.entry("templates/owner/shifts.html", "th:href=\"@{/owner}\" aria-label=\"Back to Owner dashboard\""),
			Map.entry("templates/owner/stores.html", "th:href=\"@{/owner}\" aria-label=\"Back to Owner dashboard\""),
			Map.entry("templates/owner/events.html", "th:href=\"@{/owner}\" aria-label=\"Back to Owner dashboard\""),
			Map.entry("templates/owner/promotions.html", "th:href=\"@{/owner}\" aria-label=\"Back to Owner dashboard\""),
			Map.entry("templates/public/content-detail.html", "aria-label=\"Back to Cast Profile\""),
			Map.entry("templates/public/cast-profile.html", "th:href=\"@{/}\" aria-label=\"Back to Home\""),
			Map.entry("templates/public/event-detail.html", "th:href=\"@{/}\" aria-label=\"Back to Home\"")
		);

		for (var entry : workflowParents.entrySet()) {
			assertThat(resource(entry.getKey()))
				.as(entry.getKey())
				.contains("page-back-button")
				.contains("bi-arrow-left")
				.contains(entry.getValue());
		}

		assertThat(resource("templates/member/casts.html"))
			.contains("sec:authorize=\"hasRole('CAST')\"")
			.contains("th:href=\"@{/}\" aria-label=\"Back to Home\"");
	}

	@Test
	void sharedWordmarkUsesOneProminentSafeAreaAwareTreatment() throws IOException {
		var sharedStyles = resource("static/css/member-home.css");
		var profileStyles = resource("static/css/member-profile.css");

		assertThat(sharedStyles)
			.contains("padding: calc(17px + env(safe-area-inset-top)) 15px")
			.contains(".wordmark { display: inline-flex; min-height: 38px;")
			.contains("font-size: 1.22rem")
			.contains("font-weight: 800");
		assertThat(profileStyles)
			.contains(".account-prototype .wordmark { color: var(--profile-ink); }")
			.doesNotContain(".account-prototype .wordmark { color: var(--profile-ink); font-size:");
	}

	private String resource(String path) throws IOException {
		return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
	}
}
