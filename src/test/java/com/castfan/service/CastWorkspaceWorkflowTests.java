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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import com.castfan.entity.CastProfile;
import com.castfan.entity.ShiftStatus;
import com.castfan.entity.StoreCast;
import com.castfan.entity.UserAccount;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.ShiftScheduleRepository;
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
class CastWorkspaceWorkflowTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PrivilegedAccountProvisioningService provisioningService;

	@Autowired
	private StoreService storeService;

	@Autowired
	private StoreCastService storeCastService;

	@Autowired
	private CastProfileRepository castProfileRepository;

	@Autowired
	private ShiftScheduleRepository shiftScheduleRepository;

	private UserAccount cast;
	private StoreCast assignment;

	@BeforeEach
	void setUp() {
		var suffix = Long.toString(System.nanoTime());
		var owner = provisioningService.provisionOwner(
			"cast-workspace-owner-" + suffix + "@castfan.test", "secure-owner-pass-123", "Workspace Owner"
		);
		cast = provisioningService.provisionCast(
			"cast-workspace-" + suffix + "@castfan.test", "secure-cast-pass-123", "Workspace Cast", "Mina Workspace " + suffix
		);
		var store = storeService.createStore(
			owner, "Hoshi Test Cafe", "hoshi-test-" + suffix, null, "Bangkok", "Asia/Bangkok"
		);
		assignment = storeCastService.assignCast(owner, store.getId(), requireProfile().getId());
	}

	@Test
	void castCanOpenDashboardScheduleAndProfile() throws Exception {
		mockMvc.perform(get("/cast").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isOk())
			.andExpect(view().name("cast/dashboard"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Hoshi Test Cafe")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Request shift")))
			.andExpect(content().string(org.hamcrest.Matchers.not(
				org.hamcrest.Matchers.containsString("Manage content"))))
			.andExpect(content().string(org.hamcrest.Matchers.matchesPattern(
				"(?s).*<nav class=\"floating-nav\" aria-label=\"Cast navigation\">"
					+ ".*?<span>Home</span>.*?<span>Schedule</span>.*?<span>Cast Profile</span>"
					+ ".*?<span>Profile</span>.*?<span>Cast</span>.*?</nav>.*"
			)));

		mockMvc.perform(get("/cast/schedule").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isOk())
			.andExpect(view().name("cast/schedule"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Submit a shift")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("data-picker-date")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("data-picker-time")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("date-time-picker.js")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("confirmation-dialog.js")));

		mockMvc.perform(get("/cast/profile").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isOk())
			.andExpect(view().name("cast/profile"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Your account")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Manage profile")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Preferences")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Support")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Discoverable profile")))
			.andExpect(content().string(org.hamcrest.Matchers.not(
				org.hamcrest.Matchers.containsString("/member/notifications"))))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cast/profile\"")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("bi-pencil-square")));

		mockMvc.perform(get("/cast/public-profile").with(user(cast.getEmail()).roles("CAST")))
			.andExpect(status().isOk())
			.andExpect(view().name("public/cast-profile"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString(requireProfile().getStageName())))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Cast Profile")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"profile-compose-fab\"")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/cast/content#composer\"")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("New post")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("aria-current=\"page\"")))
			.andExpect(content().string(org.hamcrest.Matchers.not(
				org.hamcrest.Matchers.containsString("<span>Follow</span>"))));
	}

	@Test
	void castCanSubmitAndCancelOwnShift() throws Exception {
		var date = LocalDate.now(ZoneId.of("Asia/Bangkok")).plusDays(3);
		mockMvc.perform(post("/cast/schedule")
				.with(user(cast.getEmail()).roles("CAST"))
				.with(csrf())
				.param("storeCastId", assignment.getId().toString())
				.param("date", date.toString())
				.param("startsAt", "14:00")
				.param("endsAt", "20:00"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/cast/schedule?submitted"));

		var shift = shiftScheduleRepository.findAllByStoreCastIdOrderByStartsAtAsc(assignment.getId()).getFirst();
		assertThat(shift.getStatus()).isEqualTo(ShiftStatus.SUBMITTED);

		mockMvc.perform(post("/cast/shifts/{id}/cancel", shift.getId())
				.with(user(cast.getEmail()).roles("CAST"))
				.with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/cast/schedule?cancelled=true"));

		assertThat(shiftScheduleRepository.findById(shift.getId()).orElseThrow().getStatus())
			.isEqualTo(ShiftStatus.CANCELLED);
	}

	@Test
	void castCanUpdateOnlyTheirOwnPublicProfile() throws Exception {
		mockMvc.perform(post("/cast/profile")
				.with(user(cast.getEmail()).roles("CAST"))
				.with(csrf())
				.param("stageName", "Updated Mina " + cast.getId())
				.param("bio", "Updated public bio")
				.param("discoverable", "true"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/cast/profile?saved"));

		var profile = requireProfile();
		assertThat(profile.getStageName()).startsWith("Updated Mina");
		assertThat(profile.getBio()).isEqualTo("Updated public bio");
		assertThat(profile.isDiscoverable()).isTrue();
	}

	@Test
	void invalidOrUnownedAssignmentIsRejectedWithoutCreatingShift() throws Exception {
		var otherCast = provisioningService.provisionCast(
			"other-cast-" + System.nanoTime() + "@castfan.test", "secure-cast-pass-123", "Other Cast", "Other Cast " + System.nanoTime()
		);
		var date = LocalDate.now(ZoneId.of("Asia/Bangkok")).plusDays(4);

		mockMvc.perform(post("/cast/schedule")
				.with(user(otherCast.getEmail()).roles("CAST"))
				.with(csrf())
				.param("storeCastId", assignment.getId().toString())
				.param("date", date.toString())
				.param("startsAt", LocalTime.of(14, 0).toString())
				.param("endsAt", LocalTime.of(20, 0).toString()))
			.andExpect(status().isOk())
			.andExpect(view().name("cast/schedule"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Store assignment not found")));

		assertThat(shiftScheduleRepository.findAllByStoreCastIdOrderByStartsAtAsc(assignment.getId())).isEmpty();
	}

	private CastProfile requireProfile() {
		return castProfileRepository.findByUserAccountId(cast.getId()).orElseThrow();
	}
}
