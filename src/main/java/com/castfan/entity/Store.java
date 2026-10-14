package com.castfan.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "stores")
public class Store {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_user_account_id", nullable = false)
	private UserAccount owner;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(nullable = false, unique = true, length = 100)
	private String slug;

	@Column(length = 1000)
	private String description;

	@Column(length = 500)
	private String address;

	@Column(nullable = false, length = 64)
	private String timezone;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Store() {
	}

	public Store(
		UserAccount owner,
		String name,
		String slug,
		String description,
		String address,
		String timezone,
		boolean active
	) {
		this.owner = owner;
		this.name = name;
		this.slug = slug;
		this.description = description;
		this.address = address;
		this.timezone = timezone;
		this.active = active;
	}

	public void updateDetails(String name, String description, String address, String timezone) {
		this.name = name;
		this.description = description;
		this.address = address;
		this.timezone = timezone;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
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

	public UserAccount getOwner() {
		return owner;
	}

	public String getName() {
		return name;
	}

	public String getSlug() {
		return slug;
	}

	public String getDescription() {
		return description;
	}

	public String getAddress() {
		return address;
	}

	public String getTimezone() {
		return timezone;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
