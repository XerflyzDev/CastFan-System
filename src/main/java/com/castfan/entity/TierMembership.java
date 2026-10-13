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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "tier_memberships",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_tier_membership_member_tier",
		columnNames = {"member_profile_id", "member_tier_id"}
	)
)
public class TierMembership {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_profile_id", nullable = false)
	private MemberProfile memberProfile;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_tier_id", nullable = false)
	private MemberTier memberTier;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MembershipStatus status;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "ended_at")
	private Instant endedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected TierMembership() {
	}

	public TierMembership(MemberProfile memberProfile, MemberTier memberTier) {
		this.memberProfile = memberProfile;
		this.memberTier = memberTier;
		this.status = MembershipStatus.ACTIVE;
		this.startedAt = Instant.now();
	}

	public void reactivate() {
		if (status == MembershipStatus.ACTIVE) {
			return;
		}
		status = MembershipStatus.ACTIVE;
		startedAt = Instant.now();
		endedAt = null;
	}

	public void cancel() {
		if (status != MembershipStatus.ACTIVE) {
			throw new IllegalStateException("Only active memberships can be cancelled.");
		}
		status = MembershipStatus.CANCELLED;
		endedAt = Instant.now();
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

	public MemberProfile getMemberProfile() {
		return memberProfile;
	}

	public MemberTier getMemberTier() {
		return memberTier;
	}

	public MembershipStatus getStatus() {
		return status;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getEndedAt() {
		return endedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
