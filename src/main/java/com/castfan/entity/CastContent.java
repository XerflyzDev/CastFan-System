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
	name = "cast_content",
	indexes = @Index(
		name = "idx_content_cast_status_published",
		columnList = "cast_profile_id, status, published_at"
	)
)
public class CastContent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cast_profile_id", nullable = false)
	private CastProfile castProfile;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(nullable = false, length = 5000)
	private String body;

	@Column(name = "image_url", length = 2048)
	private String imageUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ContentAudience audience;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "minimum_tier_id")
	private MemberTier minimumTier;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private ContentStatus status;

	@Column(name = "published_at")
	private Instant publishedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected CastContent() { }

	public CastContent(CastProfile castProfile, String title, String body, String imageUrl,
		ContentAudience audience, MemberTier minimumTier) {
		this.castProfile = castProfile;
		this.status = ContentStatus.DRAFT;
		updateDetails(title, body, imageUrl, audience, minimumTier);
	}

	public void updateDetails(String title, String body, String imageUrl,
		ContentAudience audience, MemberTier minimumTier) {
		if (status == ContentStatus.ARCHIVED) throw new IllegalStateException("Archived content cannot be edited.");
		if (audience == ContentAudience.MEMBERS && minimumTier == null) {
			throw new IllegalArgumentException("A minimum tier is required for member content.");
		}
		if (audience != ContentAudience.MEMBERS && minimumTier != null) {
			throw new IllegalArgumentException("A minimum tier can be used only for member content.");
		}
		this.title = title;
		this.body = body;
		this.imageUrl = imageUrl;
		this.audience = audience;
		this.minimumTier = minimumTier;
	}

	public void publish() {
		if (status == ContentStatus.ARCHIVED) throw new IllegalStateException("Archived content cannot be published.");
		status = ContentStatus.PUBLISHED;
		if (publishedAt == null) publishedAt = Instant.now();
	}

	public void archive() {
		if (status == ContentStatus.ARCHIVED) throw new IllegalStateException("Content is already archived.");
		status = ContentStatus.ARCHIVED;
	}

	@PrePersist
	void onCreate() { var now = Instant.now(); createdAt = now; updatedAt = now; }

	@PreUpdate
	void onUpdate() { updatedAt = Instant.now(); }

	public Long getId() { return id; }
	public CastProfile getCastProfile() { return castProfile; }
	public String getTitle() { return title; }
	public String getBody() { return body; }
	public String getImageUrl() { return imageUrl; }
	public ContentAudience getAudience() { return audience; }
	public MemberTier getMinimumTier() { return minimumTier; }
	public ContentStatus getStatus() { return status; }
	public Instant getPublishedAt() { return publishedAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
}
