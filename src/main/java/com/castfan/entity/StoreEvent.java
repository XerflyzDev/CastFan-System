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
	name = "store_events",
	indexes = @Index(name = "idx_store_event_status_start", columnList = "store_id, status, starts_at")
)
public class StoreEvent {

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

	@Column(name = "image_url", length = 2048)
	private String imageUrl;

	@Column(name = "starts_at", nullable = false)
	private Instant startsAt;

	@Column(name = "ends_at", nullable = false)
	private Instant endsAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EventStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected StoreEvent() {
	}

	public StoreEvent(
		Store store,
		UserAccount createdBy,
		String title,
		String description,
		String imageUrl,
		Instant startsAt,
		Instant endsAt,
		EventStatus status
	) {
		if (!endsAt.isAfter(startsAt)) {
			throw new IllegalArgumentException("Event end time must be after its start time.");
		}
		this.store = store;
		this.createdBy = createdBy;
		this.title = title;
		this.description = description;
		this.imageUrl = imageUrl;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.status = status;
	}

	public void updateDetails(
		String title,
		String description,
		String imageUrl,
		Instant startsAt,
		Instant endsAt
	) {
		if (status == EventStatus.ARCHIVED) {
			throw new IllegalStateException("Archived events cannot be edited.");
		}
		if (!endsAt.isAfter(startsAt)) {
			throw new IllegalArgumentException("Event end time must be after its start time.");
		}
		this.title = title;
		this.description = description;
		this.imageUrl = imageUrl;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
	}

	public void publish() {
		if (status != EventStatus.DRAFT) {
			throw new IllegalStateException("Only draft events can be published.");
		}
		status = EventStatus.PUBLISHED;
	}

	public void archive() {
		if (status == EventStatus.ARCHIVED) {
			throw new IllegalStateException("This event is already archived.");
		}
		status = EventStatus.ARCHIVED;
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

	public Long getId() { return id; }
	public Store getStore() { return store; }
	public UserAccount getCreatedBy() { return createdBy; }
	public String getTitle() { return title; }
	public String getDescription() { return description; }
	public String getImageUrl() { return imageUrl; }
	public Instant getStartsAt() { return startsAt; }
	public Instant getEndsAt() { return endsAt; }
	public EventStatus getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
