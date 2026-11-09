package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.castfan.dto.OwnerWorkspaceView;
import com.castfan.entity.CastProfile;
import com.castfan.entity.ShiftSchedule;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OwnerWorkspaceWorkflowTests {

	private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Bangkok");
	private static final DateTimeFormatter FULL_DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM uuuu", Locale.ENGLISH);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PrivilegedAccountProvisioningService provisioningService;

	@Autowired
	private StoreService storeService;

	@Autowired
	private StoreCastService storeCastService;

	@Autowired
	private ShiftScheduleService shiftScheduleService;

	@Autowired
	private CastProfileRepository castProfileRepository;

	@Autowired
	private ShiftScheduleRepository shiftScheduleRepository;

	@Autowired
	private OwnerWorkspaceService ownerWorkspaceService;

	private UserAccount owner;
	private UserAccount cast;
	private StoreCast assignment;
	private ShiftSchedule submittedShift;
	private LocalDate shiftDate;

	@BeforeEach
	void setUp() {
		var suffix = Long.toString(System.nanoTime());
		owner = provisioningService.provisionOwner(
			"owner-workspace-" + suffix + "@castfan.test", "secure-owner-pass-123", "Hoshi Owner"
		);
		cast = provisioningService.provisionCast(
			"owner-cast-" + suffix + "@castfan.test", "secure-cast-pass-123", "Mina Cast", "Mina Review " + suffix
		);
		var store = storeService.createStore(
			owner, "Hoshi Review Cafe", "hoshi-review-" + suffix, null, "Bangkok", STORE_ZONE.getId()
		);
		assignment = storeCastService.assignCast(owner, store.getId(), requireProfile(cast).getId());
		shiftDate = LocalDate.now(STORE_ZONE).plusDays(3);
		submittedShift = submit(shiftDate, LocalTime.of(14, 0), LocalTime.of(20, 0));
	}

	@Test
	void ownerCanOpenDashboardAndDateSchedule() throws Exception {
		var profile = requireProfile(cast);
		profile.updatePublicDetails(
			profile.getStageName(), profile.getStageNameKey(), profile.getBio(), "/avatars/" + cast.getId()
		);
		castProfileRepository.save(profile);

		mockMvc.perform(get("/owner").with(user(owner.getEmail()).roles("OWNER")))
			.andExpect(status().isOk())
			.andExpect(view().name("owner/dashboard"))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("CastFan")))
			.andExpect(content().string(org.hamcrest.Matchers.containsString("Mina Review")))
			.andExpect(content().string(containsString("Active Cast members")))
			.andExpect(content().string(containsString("Pending shift requests")))
			.andExpect(content().string(containsString("Shifts approved today")))
			.andExpect(content().string(containsString("Upcoming published events")))
			.andExpect(content().string(containsString("Manage Cast accounts")))
			.andExpect(content().string(containsString("Add Members as Cast and manage active Cast access")))
			.andExpect(content().string(containsString("View all requests")))
			.andExpect(content().string(containsString("Member points")))
			.andExpect(content().string(containsString("Pending review")))
			.andExpect(content().string(not(containsString("Needs review"))))
			.andExpect(content().string(not(containsString("Review schedule"))))
			.andExpect(content().string(not(containsString("Promotions &amp; banners"))));

		mockMvc.perform(get("/owner/shifts")
				.param("date", shiftDate.toString())
				.with(user(owner.getEmail()).roles("OWNER")))
			.andExpect(status().isOk())
			.andExpect(view().name("owner/shifts"))
			.andExpect(content().string(containsString("Review queue")))
			.andExpect(content().string(containsString("14:00 - 20:00")))
			.andExpect(content().string(containsString(FULL_DATE.format(shiftDate))))
			.andExpect(content().string(containsString("owner-shifts.js")))
			.andExpect(content().string(containsString("src=\"/avatars/" + cast.getId() + "\"")))
			.andExpect(content().string(containsString("owner-schedule-navigator")))
			.andExpect(content().string(containsString("owner-week-range")))
			.andExpect(content().string(containsString("Choose another date")))
			.andExpect(content().string(containsString("owner-calendar-panel")))
			.andExpect(content().string(containsString("data-owner-calendar-grid")))
			.andExpect(content().string(not(containsString("type=\"date\""))))
			.andExpect(content().string(containsString(">Today</a>")))
			.andExpect(content().string(containsString("Previous week")))
			.andExpect(content().string(containsString("Next week")))
			.andExpect(content().string(not(containsString("Advanced"))))
			.andExpect(content().string(containsString("aria-label=\"Choose a schedule date\"")))
			.andExpect(content().string(containsString("aria-current=\"date\"")))
			.andExpect(content().string(not(containsString("data-picker-date"))))
			.andExpect(content().string(not(containsString("date-time-picker.js"))))
			.andExpect(content().string(not(containsString("aria-label=\"Local time\""))));
	}

	@Test
	void dashboardExcludesExpiredSubmittedShiftsWithoutDeletingHistory() {
		var expired = shiftScheduleService.submitShift(
			cast,
			assignment.getId(),
			java.time.Instant.now().minusSeconds(7_200),
			java.time.Instant.now().minusSeconds(3_600)
		);

		var workspace = ownerWorkspaceService.load(owner.getEmail(), null);

		assertThat(workspace.pendingCount()).isEqualTo(1);
		assertThat(workspace.days()).hasSize(7);
		assertThat(workspace.days()).filteredOn(OwnerWorkspaceView.ScheduleDay::selected).hasSize(1);
		assertThat(workspace.todayDate()).isNotBlank();
		assertThat(workspace.pendingShifts()).hasSize(1);
		assertThat(workspace.pendingShifts().getFirst().id()).isEqualTo(submittedShift.getId());
		assertThat(shiftScheduleRepository.findById(expired.getId())).isPresent();
		assertThat(shiftScheduleRepository.findById(expired.getId()).orElseThrow().getStatus())
			.isEqualTo(ShiftStatus.SUBMITTED);
	}

	@Test
	void futurePendingQueryUsesStrictInstantBoundaryAcrossTheStoreTimezone() {
		var cutoff = ZonedDateTime.of(
			LocalDate.of(2026, 11, 9), LocalTime.NOON, STORE_ZONE
		).toInstant();
		var past = shiftScheduleService.submitShift(
			cast, assignment.getId(), cutoff.minusSeconds(3_600), cutoff.minusSeconds(1_800)
		);
		var boundary = shiftScheduleService.submitShift(
			cast, assignment.getId(), cutoff, cutoff.plusSeconds(1_800)
		);
		var future = shiftScheduleService.submitShift(
			cast, assignment.getId(), cutoff.plusSeconds(3_600), cutoff.plusSeconds(5_400)
		);

		var result = shiftScheduleRepository.findFutureForOwnerByStatus(
			owner.getEmail(), ShiftStatus.SUBMITTED, cutoff, org.springframework.data.domain.PageRequest.of(0, 25)
		);

		assertThat(result).extracting(ShiftSchedule::getId)
			.contains(future.getId())
			.doesNotContain(past.getId(), boundary.getId());
		assertThat(shiftScheduleRepository.countFutureForOwnerByStatus(
			owner.getEmail(), ShiftStatus.SUBMITTED, cutoff
		)).isEqualTo(result.size());
	}

	@Test
	void ownerCanApproveTheirStoreShift() throws Exception {
		mockMvc.perform(post("/owner/shifts/{id}/approve", submittedShift.getId())
				.with(user(owner.getEmail()).roles("OWNER"))
				.with(csrf())
				.param("date", shiftDate.toString())
				.param("reviewNote", "Staffing confirmed"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/shifts?approved=true&date=" + shiftDate));

		var approved = shiftScheduleRepository.findById(submittedShift.getId()).orElseThrow();
		assertThat(approved.getStatus()).isEqualTo(ShiftStatus.APPROVED);
		assertThat(approved.getReviewNote()).isEqualTo("Staffing confirmed");
	}

	@Test
	void reviewActionsChangeOnlyTheShiftIdSubmittedByEachCard() throws Exception {
		var secondShift = submit(shiftDate, LocalTime.of(8, 0), LocalTime.of(12, 0));
		mockMvc.perform(post("/owner/shifts/{id}/approve", submittedShift.getId())
				.with(user(owner.getEmail()).roles("OWNER")).with(csrf()).param("date", shiftDate.toString()))
			.andExpect(status().is3xxRedirection());
		assertThat(shiftScheduleRepository.findById(submittedShift.getId()).orElseThrow().getStatus()).isEqualTo(ShiftStatus.APPROVED);
		assertThat(shiftScheduleRepository.findById(secondShift.getId()).orElseThrow().getStatus()).isEqualTo(ShiftStatus.SUBMITTED);
		mockMvc.perform(post("/owner/shifts/{id}/reject", secondShift.getId())
				.with(user(owner.getEmail()).roles("OWNER")).with(csrf())
				.param("date", shiftDate.toString()).param("reviewNote", "Coverage not required"))
			.andExpect(status().is3xxRedirection());
		assertThat(shiftScheduleRepository.findById(submittedShift.getId()).orElseThrow().getStatus()).isEqualTo(ShiftStatus.APPROVED);
		assertThat(shiftScheduleRepository.findById(secondShift.getId()).orElseThrow().getStatus()).isEqualTo(ShiftStatus.REJECTED);
	}

	@Test
	void rejectionRequiresAReasonAndThenRecordsIt() throws Exception {
		MvcResult invalidResult = mockMvc.perform(post("/owner/shifts/{id}/reject", submittedShift.getId())
				.with(user(owner.getEmail()).roles("OWNER"))
				.with(csrf())
				.param("date", shiftDate.toString())
				.param("reviewNote", ""))
			.andExpect(status().is3xxRedirection())
			.andReturn();
		assertThat(invalidResult.getResponse().getRedirectedUrl()).contains("error=", "date=" + shiftDate);
		assertThat(shiftScheduleRepository.findById(submittedShift.getId()).orElseThrow().getStatus())
			.isEqualTo(ShiftStatus.SUBMITTED);

		mockMvc.perform(post("/owner/shifts/{id}/reject", submittedShift.getId())
				.with(user(owner.getEmail()).roles("OWNER"))
				.with(csrf())
				.param("date", shiftDate.toString())
				.param("reviewNote", "Store capacity reached"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owner/shifts?rejected=true&date=" + shiftDate));

		var rejected = shiftScheduleRepository.findById(submittedShift.getId()).orElseThrow();
		assertThat(rejected.getStatus()).isEqualTo(ShiftStatus.REJECTED);
		assertThat(rejected.getReviewNote()).isEqualTo("Store capacity reached");
	}

	@Test
	void anotherOwnerCannotReviewTheShift() throws Exception {
		var otherOwner = provisioningService.provisionOwner(
			"other-owner-" + System.nanoTime() + "@castfan.test", "secure-owner-pass-123", "Other Owner"
		);

		mockMvc.perform(post("/owner/shifts/{id}/approve", submittedShift.getId())
				.with(user(otherOwner.getEmail()).roles("OWNER"))
				.with(csrf()))
			.andExpect(status().is3xxRedirection());

		assertThat(shiftScheduleRepository.findById(submittedShift.getId()).orElseThrow().getStatus())
			.isEqualTo(ShiftStatus.SUBMITTED);
	}

	@Test
	void overlappingApprovedShiftCannotBeApproved() throws Exception {
		shiftScheduleService.approveShift(owner, submittedShift.getId(), null);
		var overlapping = submit(shiftDate, LocalTime.of(18, 0), LocalTime.of(22, 0));

		mockMvc.perform(post("/owner/shifts/{id}/approve", overlapping.getId())
				.with(user(owner.getEmail()).roles("OWNER"))
				.with(csrf())
				.param("date", shiftDate.toString()))
			.andExpect(status().is3xxRedirection());

		assertThat(shiftScheduleRepository.findById(overlapping.getId()).orElseThrow().getStatus())
			.isEqualTo(ShiftStatus.SUBMITTED);
	}

	private ShiftSchedule submit(LocalDate date, LocalTime startsAt, LocalTime endsAt) {
		return shiftScheduleService.submitShift(
			cast,
			assignment.getId(),
			date.atTime(startsAt).atZone(STORE_ZONE).toInstant(),
			date.atTime(endsAt).atZone(STORE_ZONE).toInstant()
		);
	}

	private CastProfile requireProfile(UserAccount account) {
		return castProfileRepository.findByUserAccountId(account.getId()).orElseThrow();
	}
}
