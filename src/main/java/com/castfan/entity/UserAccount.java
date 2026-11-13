package com.castfan.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, length = 254)
	private String email;

	@Column(name = "login_identifier", nullable = false, unique = true, length = 254)
	private String loginIdentifier;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Column(name = "display_name", nullable = false, length = 80)
	private String displayName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserStatus status;

	@Column(name = "must_change_password", nullable = false)
	private boolean mustChangePassword;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected UserAccount() {
	}

	public UserAccount(String email, String passwordHash, String displayName, UserRole role, UserStatus status) {
		this(email, email, passwordHash, displayName, role, status, false);
	}

	public UserAccount(String email, String loginIdentifier, String passwordHash, String displayName,
		UserRole role, UserStatus status, boolean mustChangePassword) {
		this.email = email;
		this.loginIdentifier = loginIdentifier;
		this.passwordHash = passwordHash;
		this.displayName = displayName;
		this.role = role;
		this.status = status;
		this.mustChangePassword = mustChangePassword;
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

	public String getEmail() {
		return email;
	}

	public String getLoginIdentifier() { return loginIdentifier; }

	public boolean isMustChangePassword() { return mustChangePassword; }

	public void changePassword(String passwordHash) {
		this.passwordHash = passwordHash;
		this.mustChangePassword = false;
	}

	public void resetTemporaryPassword(String passwordHash) {
		this.passwordHash = passwordHash;
		this.mustChangePassword = true;
	}

	public void suspend() { this.status = UserStatus.SUSPENDED; }

	public void activate() { this.status = UserStatus.ACTIVE; }

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public UserRole getRole() {
		return role;
	}

	public UserStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void updateDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void promoteMemberToCast() {
		if (status != UserStatus.ACTIVE || role != UserRole.MEMBER) {
			throw new IllegalStateException("Only an active Member account can be promoted to Cast.");
		}
		role = UserRole.CAST;
	}
}
