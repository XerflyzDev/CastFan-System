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
@Table(name = "store_memberships", uniqueConstraints = @UniqueConstraint(
	name = "uk_store_membership_store_member", columnNames = {"store_id", "member_profile_id"}))
public class StoreMembership {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false)
	private Store store;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_profile_id", nullable = false)
	private MemberProfile memberProfile;

	@Column(name = "points_balance", nullable = false)
	private long pointsBalance;

	@Column(name = "cast_update_notifications", nullable = false)
	private boolean castUpdateNotifications = true;

	@Column(name = "event_notifications", nullable = false)
	private boolean eventNotifications = true;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private StoreMembershipStatus status = StoreMembershipStatus.ACTIVE;

	@Column(name = "joined_at", nullable = false, updatable = false)
	private Instant joinedAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected StoreMembership() { }

	public StoreMembership(Store store, MemberProfile memberProfile) {
		this.store = store;
		this.memberProfile = memberProfile;
	}

	@PrePersist void onCreate() { var now = Instant.now(); joinedAt = now; updatedAt = now; }
	@PreUpdate void onUpdate() { updatedAt = Instant.now(); }

	public void applyPointsDelta(long delta) {
		long next;
		try { next = Math.addExact(pointsBalance, delta); }
		catch (ArithmeticException exception) { throw new IllegalArgumentException("Point balance is outside the supported range.", exception); }
		if (next < 0) throw new IllegalArgumentException("Point balance cannot be negative.");
		pointsBalance = next;
	}

	public void updateNotifications(boolean castUpdates, boolean events) {
		castUpdateNotifications = castUpdates;
		eventNotifications = events;
	}

	public Long getId() { return id; }
	public Store getStore() { return store; }
	public MemberProfile getMemberProfile() { return memberProfile; }
	public long getPointsBalance() { return pointsBalance; }
	public StoreMembershipTier getTier() { return StoreMembershipTier.fromPoints(pointsBalance); }
	public boolean isCastUpdateNotifications() { return castUpdateNotifications; }
	public boolean isEventNotifications() { return eventNotifications; }
	public StoreMembershipStatus getStatus() { return status; }
	public Instant getJoinedAt() { return joinedAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
