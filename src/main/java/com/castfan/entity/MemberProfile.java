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

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
