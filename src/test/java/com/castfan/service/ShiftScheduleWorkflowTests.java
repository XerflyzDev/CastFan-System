package com.castfan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.castfan.entity.CastProfile;
import com.castfan.entity.ShiftStatus;
import com.castfan.entity.Store;
import com.castfan.entity.StoreCast;
import com.castfan.entity.UserAccount;
import com.castfan.repository.CastProfileRepository;
import com.castfan.repository.ShiftScheduleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShiftScheduleWorkflowTests {

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

	@Test
	void storeOwnerCanAssignCastAndAnotherOwnerCannot() {
		var owner = provisionOwner("assignment-owner@castfan.test");
		var otherOwner = provisionOwner("assignment-other-owner@castfan.test");
		var cast = provisionCast("assignment-cast@castfan.test", "Assignment Cast");
		var store = createStore(owner, "assignment-store");
		var profile = requireCastProfile(cast);

		var assignment = storeCastService.assignCast(owner, store.getId(), profile.getId());

		assertThat(assignment.isActive()).isTrue();
		assertThat(assignment.getStore().getId()).isEqualTo(store.getId());
		assertThatThrownBy(() -> storeCastService.assignCast(otherOwner, store.getId(), profile.getId()))
			.isInstanceOf(DomainRuleViolationException.class);
		assertThatThrownBy(() -> storeCastService.assignCast(owner, store.getId(), profile.getId()))
			.isInstanceOf(DomainConflictException.class);
	}

	@Test
	void onlyAssignedCastCanSubmitAValidShift() {
		var owner = provisionOwner("submit-owner@castfan.test");
		var cast = provisionCast("submit-cast@castfan.test", "Submit Cast");
		var otherCast = provisionCast("submit-other-cast@castfan.test", "Other Submit Cast");
		var assignment = assign(owner, cast, "submit-store");
		var startsAt = Instant.parse("2026-11-10T03:00:00Z");
		var endsAt = Instant.parse("2026-11-10T09:00:00Z");

		var shift = shiftScheduleService.submitShift(cast, assignment.getId(), startsAt, endsAt);

		assertThat(shift.getStatus()).isEqualTo(ShiftStatus.SUBMITTED);
		assertThat(shift.getStartsAt()).isEqualTo(startsAt);
		assertThatThrownBy(() ->
			shiftScheduleService.submitShift(otherCast, assignment.getId(), startsAt, endsAt)
		).isInstanceOf(DomainRuleViolationException.class);
		assertThatThrownBy(() ->
			shiftScheduleService.submitShift(cast, assignment.getId(), endsAt, startsAt)
		).isInstanceOf(DomainRuleViolationException.class);
	}

	@Test
	void ownerCanApproveButCannotApproveAnOverlappingShift() {
		var owner = provisionOwner("approve-owner@castfan.test");
		var cast = provisionCast("approve-cast@castfan.test", "Approve Cast");
		var assignment = assign(owner, cast, "approve-store");
		var first = shiftScheduleService.submitShift(
			cast,
			assignment.getId(),
			Instant.parse("2026-11-11T03:00:00Z"),
			Instant.parse("2026-11-11T08:00:00Z")
		);
		var overlapping = shiftScheduleService.submitShift(
			cast,
			assignment.getId(),
			Instant.parse("2026-11-11T07:00:00Z"),
			Instant.parse("2026-11-11T10:00:00Z")
		);

		var approved = shiftScheduleService.approveShift(owner, first.getId(), "Confirmed");

		assertThat(approved.getStatus()).isEqualTo(ShiftStatus.APPROVED);
		assertThat(approved.getReviewedBy().getId()).isEqualTo(owner.getId());
		assertThat(approved.getReviewNote()).isEqualTo("Confirmed");
		assertThatThrownBy(() -> shiftScheduleService.approveShift(owner, overlapping.getId(), null))
			.isInstanceOf(DomainConflictException.class);
	}

	@Test
	void onlyStoreOwnerCanReviewAndAssignedCastCanCancel() {
		var owner = provisionOwner("review-owner@castfan.test");
		var otherOwner = provisionOwner("review-other-owner@castfan.test");
		var cast = provisionCast("review-cast@castfan.test", "Review Cast");
		var otherCast = provisionCast("review-other-cast@castfan.test", "Other Review Cast");
		var assignment = assign(owner, cast, "review-store");
		var shift = shiftScheduleService.submitShift(
			cast,
			assignment.getId(),
			Instant.parse("2026-11-12T03:00:00Z"),
			Instant.parse("2026-11-12T09:00:00Z")
		);

		assertThatThrownBy(() -> shiftScheduleService.rejectShift(otherOwner, shift.getId(), "No"))
			.isInstanceOf(DomainRuleViolationException.class);
		assertThatThrownBy(() -> shiftScheduleService.cancelShift(otherCast, shift.getId()))
			.isInstanceOf(DomainRuleViolationException.class);

		var cancelled = shiftScheduleService.cancelShift(cast, shift.getId());
		assertThat(cancelled.getStatus()).isEqualTo(ShiftStatus.CANCELLED);
		assertThat(shiftScheduleRepository.findById(shift.getId())).contains(cancelled);
	}

	@Test
	void rejectionRecordsReviewerAndPreventsAnotherReview() {
		var owner = provisionOwner("reject-owner@castfan.test");
		var cast = provisionCast("reject-cast@castfan.test", "Reject Cast");
		var assignment = assign(owner, cast, "reject-store");
		var shift = shiftScheduleService.submitShift(
			cast,
			assignment.getId(),
			Instant.parse("2026-11-13T03:00:00Z"),
			Instant.parse("2026-11-13T09:00:00Z")
		);

		var rejected = shiftScheduleService.rejectShift(owner, shift.getId(), "Store is closed");

		assertThat(rejected.getStatus()).isEqualTo(ShiftStatus.REJECTED);
		assertThat(rejected.getReviewedAt()).isNotNull();
		assertThatThrownBy(() -> shiftScheduleService.approveShift(owner, shift.getId(), null))
			.isInstanceOf(DomainRuleViolationException.class);
	}

	private UserAccount provisionOwner(String email) {
		return provisioningService.provisionOwner(email, "secure-owner-pass-123", email);
	}

	private UserAccount provisionCast(String email, String stageName) {
		return provisioningService.provisionCast(email, "secure-cast-pass-123", email, stageName);
	}

	private CastProfile requireCastProfile(UserAccount cast) {
		return castProfileRepository.findByUserAccountId(cast.getId()).orElseThrow();
	}

	private Store createStore(UserAccount owner, String slug) {
		return storeService.createStore(owner, "Test Store", slug, null, null, "Asia/Bangkok");
	}

	private StoreCast assign(UserAccount owner, UserAccount cast, String slug) {
		var store = createStore(owner, slug);
		return storeCastService.assignCast(owner, store.getId(), requireCastProfile(cast).getId());
	}
}
