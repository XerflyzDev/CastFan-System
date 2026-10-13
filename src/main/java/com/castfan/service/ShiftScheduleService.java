package com.castfan.service;

import java.time.Instant;

import com.castfan.entity.ShiftSchedule;
import com.castfan.entity.ShiftStatus;
import com.castfan.entity.UserAccount;
import com.castfan.entity.UserRole;
import com.castfan.entity.UserStatus;
import com.castfan.repository.ShiftScheduleRepository;
import com.castfan.repository.StoreCastRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftScheduleService {

	private final StoreCastRepository storeCastRepository;
	private final ShiftScheduleRepository shiftScheduleRepository;

	public ShiftScheduleService(
		StoreCastRepository storeCastRepository,
		ShiftScheduleRepository shiftScheduleRepository
	) {
		this.storeCastRepository = storeCastRepository;
		this.shiftScheduleRepository = shiftScheduleRepository;
	}

	@Transactional
	public ShiftSchedule submitShift(UserAccount castAccount, Long storeCastId, Instant startsAt, Instant endsAt) {
		requireActiveRole(castAccount, UserRole.CAST);
		requireValidRange(startsAt, endsAt);
		var assignment = storeCastRepository.findById(storeCastId)
			.orElseThrow(() -> new DomainRuleViolationException("Store assignment not found."));
		if (!assignment.isActive()) {
			throw new DomainRuleViolationException("Store assignment is inactive.");
		}
		if (!assignment.getCastProfile().getUserAccount().getId().equals(castAccount.getId())) {
			throw new DomainRuleViolationException("Cast can submit shifts only for their own assignment.");
		}

		return shiftScheduleRepository.saveAndFlush(new ShiftSchedule(assignment, startsAt, endsAt));
	}

	@Transactional
	public ShiftSchedule approveShift(UserAccount owner, Long shiftId, String reviewNote) {
		requireActiveRole(owner, UserRole.OWNER);
		var shift = requireShiftForUpdate(shiftId);
		var assignment = storeCastRepository.findByIdForUpdate(shift.getStoreCast().getId())
			.orElseThrow(() -> new DomainRuleViolationException("Store assignment not found."));
		requireStoreOwner(owner, assignment.getStore().getOwner());
		if (shift.getStatus() != ShiftStatus.SUBMITTED) {
			throw new DomainRuleViolationException("Only submitted shifts can be approved.");
		}
		if (shiftScheduleRepository.existsOverlap(
			assignment.getId(), ShiftStatus.APPROVED, shift.getStartsAt(), shift.getEndsAt()
		)) {
			throw new DomainConflictException("This shift overlaps an approved shift for the same Cast assignment.");
		}

		shift.approve(owner, cleanNote(reviewNote));
		return shiftScheduleRepository.saveAndFlush(shift);
	}

	@Transactional
	public ShiftSchedule rejectShift(UserAccount owner, Long shiftId, String reviewNote) {
		requireActiveRole(owner, UserRole.OWNER);
		var shift = requireShiftForUpdate(shiftId);
		requireStoreOwner(owner, shift.getStoreCast().getStore().getOwner());
		if (shift.getStatus() != ShiftStatus.SUBMITTED) {
			throw new DomainRuleViolationException("Only submitted shifts can be rejected.");
		}

		shift.reject(owner, cleanNote(reviewNote));
		return shiftScheduleRepository.saveAndFlush(shift);
	}

	@Transactional
	public ShiftSchedule cancelShift(UserAccount castAccount, Long shiftId) {
		requireActiveRole(castAccount, UserRole.CAST);
		var shift = requireShiftForUpdate(shiftId);
		if (!shift.getStoreCast().getCastProfile().getUserAccount().getId().equals(castAccount.getId())) {
			throw new DomainRuleViolationException("Cast can cancel only their own shift.");
		}
		try {
			shift.cancel();
		} catch (IllegalStateException exception) {
			throw new DomainRuleViolationException(exception.getMessage());
		}
		return shiftScheduleRepository.saveAndFlush(shift);
	}

	private ShiftSchedule requireShiftForUpdate(Long shiftId) {
		return shiftScheduleRepository.findByIdForUpdate(shiftId)
			.orElseThrow(() -> new DomainRuleViolationException("Shift not found."));
	}

	private void requireStoreOwner(UserAccount actor, UserAccount storeOwner) {
		if (!storeOwner.getId().equals(actor.getId())) {
			throw new DomainRuleViolationException("Only the Store owner can review this shift.");
		}
	}

	private void requireActiveRole(UserAccount account, UserRole role) {
		if (account == null || account.getId() == null || account.getStatus() != UserStatus.ACTIVE || account.getRole() != role) {
			throw new DomainRuleViolationException("An active " + role.name() + " account is required.");
		}
	}

	private void requireValidRange(Instant startsAt, Instant endsAt) {
		if (startsAt == null || endsAt == null || !endsAt.isAfter(startsAt)) {
			throw new DomainRuleViolationException("Shift end time must be after its start time.");
		}
	}

	private String cleanNote(String note) {
		if (note == null || note.isBlank()) {
			return null;
		}
		var cleaned = note.trim();
		if (cleaned.length() > 500) {
			throw new DomainRuleViolationException("Review note is too long.");
		}
		return cleaned;
	}
}
