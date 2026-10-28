package com.castfan.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "member_profiles",
	uniqueConstraints = @UniqueConstraint(name = "uk_member_profile_user", columnNames = "user_account_id")
)
public class MemberProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_account_id", nullable = false)
	private UserAccount userAccount;

	@Column(length = 500)
	private String bio;

	@Column(name = "avatar_url", length = 2048)
	private String avatarUrl;

	@Column(name = "cast_update_notifications", nullable = false)
	private boolean castUpdateNotifications = true;

	@Column(name = "event_notifications", nullable = false)
	private boolean eventNotifications = true;

	@Column(name = "points_balance", nullable = false)
	private long pointsBalance;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MemberProfile() {
	}

	public MemberProfile(UserAccount userAccount, String bio, String avatarUrl) {
		this.userAccount = userAccount;
		this.bio = bio;
		this.avatarUrl = avatarUrl;
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

	public UserAccount getUserAccount() {
		return userAccount;
	}

	public String getBio() {
		return bio;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void updateDetails(String bio, String avatarUrl, boolean castUpdateNotifications,
		boolean eventNotifications) {
		this.bio = bio;
		this.avatarUrl = avatarUrl;
		this.castUpdateNotifications = castUpdateNotifications;
		this.eventNotifications = eventNotifications;
	}

	public boolean isCastUpdateNotifications() { return castUpdateNotifications; }
	public boolean isEventNotifications() { return eventNotifications; }

	public long getPointsBalance() { return pointsBalance; }

	public StoreMembershipTier getStoreMembershipTier() {
		return StoreMembershipTier.fromPoints(pointsBalance);
	}

	public void applyPointsDelta(long pointsDelta) {
		long resultingBalance;
		try {
			resultingBalance = Math.addExact(pointsBalance, pointsDelta);
		} catch (ArithmeticException exception) {
			throw new IllegalArgumentException("Point balance is outside the supported range.", exception);
		}
		if (resultingBalance < 0) {
			throw new IllegalArgumentException("Point balance cannot be negative.");
		}
		pointsBalance = resultingBalance;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
