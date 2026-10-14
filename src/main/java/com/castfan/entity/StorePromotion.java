package com.castfan.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "store_promotions",
	indexes = @Index(name = "idx_store_promotion_active_range", columnList = "store_id, active, starts_at, ends_at"),
	uniqueConstraints = @UniqueConstraint(name = "uk_store_promotion_code", columnNames = "code")
)
public class StorePromotion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_id", nullable = false)
	private Store store;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	private UserAccount createdBy;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(nullable = false, length = 2000)
	private String description;

	@Column(length = 64)
	private String code;

	@Column(name = "banner_url", length = 2048)
	private String bannerUrl;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at", nullable = false)
	private Instant endsAt;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected StorePromotion() { }

	public StorePromotion(Store store, UserAccount createdBy, String title, String description, String code,
		String bannerUrl, Instant startsAt, Instant endsAt, boolean active) {
		validateRange(startsAt, endsAt);
		this.store = store;
		this.createdBy = createdBy;
		this.title = title;
		this.description = description;
		this.code = code;
		this.bannerUrl = bannerUrl;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.active = active;
	}

	public void updateDetails(String title, String description, String code, String bannerUrl,
		Instant startsAt, Instant endsAt) {
		validateRange(startsAt, endsAt);
		this.title = title;
		this.description = description;
		this.code = code;
		this.bannerUrl = bannerUrl;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
	}

	public void activate() { active = true; }
	public void deactivate() { active = false; }

	private static void validateRange(Instant startsAt, Instant endsAt) {
		if (!endsAt.isAfter(startsAt)) {
			throw new IllegalArgumentException("Promotion end time must be after its start time.");
		}
	}

	@PrePersist
	void onCreate() {
		var now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() { updatedAt = Instant.now(); }

	public Long getId() { return id; }
	public Store getStore() { return store; }
	public UserAccount getCreatedBy() { return createdBy; }
	public String getTitle() { return title; }
	public String getDescription() { return description; }
	public String getCode() { return code; }
	public String getBannerUrl() { return bannerUrl; }
	public Instant getStartsAt() { return startsAt; }
	public Instant getEndsAt() { return endsAt; }
	public boolean isActive() { return active; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
