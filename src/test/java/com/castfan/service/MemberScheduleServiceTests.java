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

		assertThat(schedule.storeName()).isEqualTo("Hoshi Cafe");
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

	private UserAccount account(String email, String name, UserRole role) {
		return userAccountRepository.save(new UserAccount(email, "encoded", name, role, UserStatus.ACTIVE));
	}

	private StoreCast assignment(Store store, String email, String stageName) {
		var castAccount = account(email, stageName, UserRole.CAST);
		var profile = castProfileRepository.save(new CastProfile(
			castAccount, stageName, stageName.toLowerCase().replace(' ', '-'), null, null, true
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
