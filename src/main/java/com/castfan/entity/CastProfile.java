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
	name = "cast_profiles",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_cast_profile_user", columnNames = "user_account_id"),
		@UniqueConstraint(name = "uk_cast_profile_stage_name", columnNames = "stage_name_key")
	}
)
public class CastProfile {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_account_id", nullable = false)
	private UserAccount userAccount;

	@Column(name = "stage_name", nullable = false, length = 80)
	private String stageName;

	@Column(name = "stage_name_key", nullable = false, length = 100)
	private String stageNameKey;

	@Column(length = 1000)
	private String bio;

	@Column(name = "avatar_url", length = 2048)
	private String avatarUrl;

	@Column(nullable = false)
	private boolean discoverable = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CastProfile() {
	}

	public CastProfile(
		UserAccount userAccount,
		String stageName,
		String stageNameKey,
		String bio,
		String avatarUrl
	) {
		this.userAccount = userAccount;
		this.stageName = stageName;
		this.stageNameKey = stageNameKey;
		this.bio = bio;
		this.avatarUrl = avatarUrl;
		this.discoverable = true;
	}

	public void updatePublicDetails(
		String stageName,
		String stageNameKey,
		String bio,
		String avatarUrl
	) {
		this.stageName = stageName;
		this.stageNameKey = stageNameKey;
		this.bio = bio;
		this.avatarUrl = avatarUrl;
		this.discoverable = true;
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

	public String getStageName() {
		return stageName;
	}

	public String getStageNameKey() {
		return stageNameKey;
	}

	public String getBio() {
		return bio;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
