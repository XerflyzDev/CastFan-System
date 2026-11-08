package com.castfan.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
	indexes = @Index(name = "idx_store_promotion_status_range", columnList = "store_id, status, starts_at, ends_at"),
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

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PromotionStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected StorePromotion() { }

	public StorePromotion(Store store, UserAccount createdBy, String title, String description, String code,
		String bannerUrl, Instant startsAt, Instant endsAt, PromotionStatus status) {
		validateRange(startsAt, endsAt);
		if (status == null) {
			throw new IllegalArgumentException("Promotion status is required.");
		}
		this.store = store;
		this.createdBy = createdBy;
		this.title = title;
		this.description = description;
		this.code = code;
		this.bannerUrl = bannerUrl;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.status = status;
	}

	public void updateDetails(String title, String description, String code, String bannerUrl,
		Instant startsAt, Instant endsAt) {
		if (status == PromotionStatus.ARCHIVED) {
			throw new IllegalStateException("Archived promotions cannot be edited.");
		}
		if (!endsAt.isAfter(Instant.now()) || !this.endsAt.isAfter(Instant.now())) {
			throw new IllegalStateException("Expired promotions cannot be edited.");
		}
		validateRange(startsAt, endsAt);
		this.title = title;
		this.description = description;
		this.code = code;
		this.bannerUrl = bannerUrl;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
	}

	public void publish() {
		if (status != PromotionStatus.DRAFT) {
			throw new IllegalStateException("Only draft promotions can be published.");
		}
		status = PromotionStatus.ACTIVE;
	}

	public void archive() {
		if (status == PromotionStatus.ARCHIVED) {
			throw new IllegalStateException("This promotion is already archived.");
		}
		status = PromotionStatus.ARCHIVED;
	}

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
	public PromotionStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
