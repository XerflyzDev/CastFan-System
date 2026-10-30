package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.ZoneId;

import com.castfan.entity.CastProfile;
import com.castfan.entity.ShiftSchedule;
import com.castfan.entity.Store;
import com.castfan.entity.StoreCast;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreCastRepository;
import com.castfan.repository.StoreRepository;
import com.castfan.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberScheduleServiceTests {

	private static final ZoneId STORE_ZONE = ZoneId.of("Asia/Bangkok");

	@Autowired private MemberScheduleService memberScheduleService;
	@Autowired private UserAccountRepository userAccountRepository;
	@Autowired private CastProfileRepository castProfileRepository;
	@Autowired private StoreRepository storeRepository;
	@Autowired private StoreCastRepository storeCastRepository;
	@Autowired private ShiftScheduleRepository shiftScheduleRepository;

	@Test
	void scheduleUsesSelectedDateAndOnlyApprovedShiftsFromTheActiveStore() {
		var owner = account("owner-schedule@test.local", "Owner", UserRole.OWNER);
		var otherOwner = account("other-owner-schedule@test.local", "Other Owner", UserRole.OWNER);
		var store = storeRepository.save(new Store(
			owner, "Hoshi Cafe", "schedule-hoshi", null, null, STORE_ZONE.getId(), true
		));
		var otherStore = storeRepository.save(new Store(
			otherOwner, "Other Cafe", "schedule-other", null, null, STORE_ZONE.getId(), true
		));
		var selectedDate = LocalDate.of(2026, 10, 14);

		var minaAssignment = assignment(store, "mina-schedule@test.local", "Mina");
		var yukiAssignment = assignment(store, "yuki-schedule@test.local", "Yuki");
		var otherAssignment = assignment(otherStore, "other-cast-schedule@test.local", "Other Cast");
		approvedShift(minaAssignment, owner, selectedDate, 14, 20);
		shiftScheduleRepository.save(new ShiftSchedule(
			yukiAssignment,
			selectedDate.atTime(16, 0).atZone(STORE_ZONE).toInstant(),
			selectedDate.atTime(22, 0).atZone(STORE_ZONE).toInstant()
		));
		approvedShift(otherAssignment, otherOwner, selectedDate, 12, 18);

		var schedule = memberScheduleService.load(selectedDate);

		assertThat(schedule.storeName()).isEqualTo("CastFan");
		assertThat(schedule.selectedDate()).isEqualTo("2026-10-14");
		assertThat(schedule.selectedDateLabel()).isEqualTo("Wednesday, 14 October");
		assertThat(schedule.closingTime()).isEqualTo("20:00");
		assertThat(schedule.shifts()).singleElement().satisfies(shift -> {
			assertThat(shift.name()).isEqualTo("Mina");
			assertThat(shift.shiftTime()).isEqualTo("14:00 - 20:00");
		});
		assertThat(schedule.days()).hasSize(7);
		assertThat(schedule.days()).filteredOn(day -> day.selected())
			.singleElement().extracting(day -> day.isoDate()).isEqualTo("2026-10-14");
	}

	@Test
	void scheduleRejectsDatesOutsideTheSupportedRange() {
		assertThatThrownBy(() -> memberScheduleService.load(LocalDate.MAX))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("400 BAD_REQUEST");
	}

	@Test
	void weekViewGroupsApprovedShiftsAcrossSevenDays() {
		var owner = account("week-owner@test.local", "Week Owner", UserRole.OWNER);
		var store = storeRepository.save(new Store(
			owner, "Week Cafe", "week-cafe", null, null, STORE_ZONE.getId(), true
		));
		var selectedDate = LocalDate.of(2026, 10, 14);
		var minaAssignment = assignment(store, "week-mina@test.local", "Week Mina");
		var yukiAssignment = assignment(store, "week-yuki@test.local", "Week Yuki");
		approvedShift(minaAssignment, owner, selectedDate, 14, 20);
		approvedShift(yukiAssignment, owner, selectedDate.plusDays(2), 16, 22);

		var schedule = memberScheduleService.load(selectedDate, true);

		assertThat(schedule.weekView()).isTrue();
		assertThat(schedule.visibleShiftCount()).isEqualTo(2);
		assertThat(schedule.weekSchedule()).hasSize(7);
		assertThat(schedule.weekSchedule()).filteredOn(day -> !day.shifts().isEmpty())
			.extracting(day -> day.isoDate()).containsExactly("2026-10-14", "2026-10-16");
		assertThat(schedule.previousWeekDate()).isEqualTo("2026-10-04");
		assertThat(schedule.nextWeekDate()).isEqualTo("2026-10-18");
	}

	@Test
	void approvedShiftRemainsVisibleWhenCastProfileIsNotDiscoverable() {
		var owner = account("hidden-cast-owner@test.local", "Owner", UserRole.OWNER);
		var store = storeRepository.save(new Store(
			owner, "Hidden Cast Cafe", "hidden-cast-cafe", null, null, STORE_ZONE.getId(), true
		));
		var selectedDate = LocalDate.of(2026, 10, 30);
		var assignment = assignment(store, "hidden-cast@test.local", "Hidden Cast", false);
		approvedShift(assignment, owner, selectedDate, 17, 23);

		var schedule = memberScheduleService.load(selectedDate);

		assertThat(schedule.shifts()).singleElement().satisfies(shift -> {
			assertThat(shift.name()).isEqualTo("Hidden Cast");
			assertThat(shift.shiftTime()).isEqualTo("17:00 - 23:00");
			assertThat(shift.publicProfileVisible()).isFalse();
		});
	}

	private UserAccount account(String email, String name, UserRole role) {
		return userAccountRepository.save(new UserAccount(email, "encoded", name, role, UserStatus.ACTIVE));
	}

	private StoreCast assignment(Store store, String email, String stageName) {
		return assignment(store, email, stageName, true);
	}

	private StoreCast assignment(Store store, String email, String stageName, boolean discoverable) {
		var castAccount = account(email, stageName, UserRole.CAST);
		var profile = castProfileRepository.save(new CastProfile(
			castAccount, stageName, stageName.toLowerCase().replace(' ', '-'), null, null, discoverable
		));
		return storeCastRepository.save(new StoreCast(store, profile, true));
	}

	private void approvedShift(StoreCast assignment, UserAccount owner, LocalDate date, int startsAt, int endsAt) {
		var shift = new ShiftSchedule(
			assignment,
			date.atTime(startsAt, 0).atZone(STORE_ZONE).toInstant(),
			date.atTime(endsAt, 0).atZone(STORE_ZONE).toInstant()
		);
		shift.approve(owner, null);
		shiftScheduleRepository.save(shift);
	}
}
