package com.castfan.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(
	name = "shift_schedules",
	indexes = {
		@Index(name = "idx_shift_assignment_start", columnList = "store_cast_id, starts_at"),
		@Index(name = "idx_shift_status_start", columnList = "status, starts_at")
	}
)
public class ShiftSchedule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_cast_id", nullable = false)
	private StoreCast storeCast;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at", nullable = false)
	private Instant endsAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ShiftStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by_user_id")
	private UserAccount reviewedBy;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "review_note", length = 500)
	private String reviewNote;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ShiftSchedule() {
	}

	public ShiftSchedule(StoreCast storeCast, Instant startsAt, Instant endsAt) {
		this.storeCast = storeCast;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.status = ShiftStatus.SUBMITTED;
	}

	public void approve(UserAccount reviewer, String note) {
		requireSubmitted();
		status = ShiftStatus.APPROVED;
		reviewedBy = reviewer;
		reviewedAt = Instant.now();
		reviewNote = note;
	}

	public void reject(UserAccount reviewer, String note) {
		requireSubmitted();
		status = ShiftStatus.REJECTED;
		reviewedBy = reviewer;
		reviewedAt = Instant.now();
		reviewNote = note;
	}

	public void cancel() {
		if (status != ShiftStatus.SUBMITTED && status != ShiftStatus.APPROVED) {
			throw new IllegalStateException("Only submitted or approved shifts can be cancelled.");
		}
		status = ShiftStatus.CANCELLED;
	}

	private void requireSubmitted() {
		if (status != ShiftStatus.SUBMITTED) {
			throw new IllegalStateException("Only submitted shifts can be reviewed.");
		}
	}

	@PrePersist
	void onCreate() {
		var now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public StoreCast getStoreCast() {
		return storeCast;
	}

	public Instant getStartsAt() {
		return startsAt;
	}

	public Instant getEndsAt() {
		return endsAt;
	}

	public ShiftStatus getStatus() {
		return status;
	}

	public UserAccount getReviewedBy() {
		return reviewedBy;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public String getReviewNote() {
		return reviewNote;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
