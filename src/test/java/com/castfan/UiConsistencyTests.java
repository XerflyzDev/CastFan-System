package com.castfan;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class UiConsistencyTests {

	@Test
	void castNavigationKeepsTheApprovedLabelsAndOrder() throws IOException {
		var navigation = resource("templates/fragments/navigation.html");
		var castNavigation = navigation.substring(
			navigation.indexOf("th:fragment=\"cast(active)\""),
			navigation.indexOf("th:fragment=\"owner(active)\"")
		);

		assertThat(castNavigation)
			.contains("class=\"floating-nav cast-nav\"")
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
	void fiveItemRoleNavigationKeepsReadableLabelsAt320Pixels() throws IOException {
		var sharedStyles = resource("static/css/member-home.css");

		assertThat(sharedStyles)
			.contains("@media (max-width: 350px)")
			.contains(".cast-nav .nav-button span")
			.contains(".owner-nav .nav-button span")
			.contains("font-size: 0.5rem");
	}

	@Test
	void scheduleTimelineMarkersStayCenteredOnTheTimeline() throws IOException {
		assertThat(resource("static/css/member-schedule.css"))
			.contains(".timeline::before")
			.contains(".timeline-marker")
			.contains("justify-self: center");
	}

	@Test
	void notificationFollowingLinkUsesTheSharedTextActionStyle() throws IOException {
		assertThat(resource("templates/member/notifications.html"))
			.contains("class=\"notification-section-link\"")
			.contains("bi-arrow-right");
		assertThat(resource("static/css/member-notifications.css"))
			.contains(".notification-section .section-title > .notification-section-link")
			.contains("width: auto")
			.contains("background: transparent")
			.contains("font-size: 0.58rem");
	}

	@Test
	void memberProfileEditorKeepsOneFocusedEditingWorkflow() throws IOException {
		assertThat(resource("templates/member/profile.html"))
			.contains("phone member-edit-page")
			.contains("class=\"edit-profile-summary\"")
			.contains("Save profile")
			.doesNotContain("member-overview edit-preview")
			.doesNotContain("Notification center")
			.doesNotContain("data-notification-preference");
		assertThat(resource("static/css/member-profile.css"))
			.contains(".member-edit-page .profile-form-card")
			.contains(".edit-profile-summary")
			.contains("padding: calc(17px + env(safe-area-inset-top)) 15px calc(126px + env(safe-area-inset-bottom))");
	}

	@Test
	void accountNotificationRowOpensDedicatedSettings() throws IOException {
		var account = resource("templates/account/index.html");
		var labelIndex = account.indexOf("<strong>Notifications</strong>");
		var rowStart = account.lastIndexOf("<a class=\"setting-row\"", labelIndex);
		var notificationRow = account.substring(rowStart, account.indexOf("</a>", labelIndex) + 4);

		assertThat(notificationRow)
			.startsWith("<a class=\"setting-row\"")
			.contains("th:href=\"@{/member/notification-settings}\"")
			.contains("Choose Cast updates and event reminders");
		assertThat(resource("templates/member/notification-settings.html"))
			.contains("Choose your updates")
			.contains("Changes are saved automatically")
			.contains("data-notification-preference")
			.contains("th:href=\"@{/member/notifications}\"");
	}

	@Test
	void workingCastCarouselSupportsTouchAndMouseDragging() throws IOException {
		assertThat(resource("templates/index.html"))
			.contains("class=\"cast-carousel\"")
			.contains("data-drag-scroll")
			.contains("Swipe or drag horizontally");
		assertThat(resource("static/js/member-home.js"))
			.contains("pointerdown")
			.contains("pointermove")
			.contains("setPointerCapture")
			.contains("suppressClick");
		assertThat(resource("static/css/member-home.css"))
			.contains(".cast-carousel.is-dragging")
			.contains("scroll-snap-type: x proximity")
			.contains("-webkit-overflow-scrolling: touch");
	}

	@Test
	void discoverCastUsesTheSharedNeutralTheme() throws IOException {
		assertThat(resource("templates/member/casts.html"))
			.contains("shared-page-shell discover-cast-page")
			.contains("Member discovery")
			.contains("class=\"cast-directory-item\"");
		assertThat(resource("static/css/member-home.css"))
			.contains(".discover-cast-page .ambient-top")
			.contains("background: linear-gradient(180deg, #f6f6f7")
			.contains(".discover-cast-page .wordmark")
			.contains(".discover-cast-page .cast-directory-item")
			.contains("grid-template-columns: 58px minmax(0, 1fr) auto")
			.contains("border-bottom: 1px solid var(--line)")
			.contains(".discover-cast-page .follow-button")
			.contains("background: #f7edf1");
	}

	@Test
	void activeRolePagesDoNotExposeMultiStoreSelectors() throws IOException {
		for (var path : new String[] {
			"templates/cast/schedule.html",
			"templates/owner/events.html",
			"templates/owner/promotions.html",
			"templates/owner/shifts.html"
		}) {
			assertThat(resource(path))
				.as(path)
				.doesNotContain("th:field=\"*{storeId}\"")
				.doesNotContain("name=\"storeId\"")
				.doesNotContain("Select store");
		}
	}

	@Test
	void activeMembershipPagesUseStoreWidePointsLanguage() throws IOException {
		assertThat(resource("templates/account/index.html"))
			.contains("class=\"membership-card\"")
			.contains("CASTFAN MEMBERSHIP")
			.contains("View membership points and tier");
		assertThat(resource("static/css/member-profile.css"))
			.contains(".account-prototype .membership-card")
			.contains("@media (max-width: 350px)");
		assertThat(resource("templates/member/memberships.html"))
			.contains("shared-page-shell membership-points-page")
			.contains("Membership points")
			.contains("class=\"points-summary-card\"")
			.contains("class=\"tier-progress\"")
			.contains("Current ")
			.contains("Tier 2 at 5,000")
			.contains("class=\"points-history-list\"")
			.contains("Point history")
			.doesNotContain("Your tier is based on points earned across the store")
			.doesNotContain("0 entries")
			.doesNotContain("/subscribe")
			.doesNotContain("Available tiers");
		assertThat(resource("static/css/member-home.css"))
			.contains(".membership-points-page .ambient-top")
			.contains(".points-summary-card")
			.contains(".point-transaction");
		assertThat(resource("templates/cast/content.html"))
			.contains("minimumStoreTierRank")
			.doesNotContain("minimumTierId");
	}

	@Test
	void castShiftSubmissionExplainsTheOwnerApprovalState() throws IOException {
		assertThat(resource("templates/cast/schedule.html"))
			.contains("Pending approval from the store Owner")
			.contains("Your request goes to the store Owner for approval")
			.contains("It stays Pending until approved");
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
