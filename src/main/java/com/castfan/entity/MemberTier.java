package com.castfan.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Check;

@Entity
@Check(constraints = "rank_number > 0 AND monthly_price >= 0")
@Table(
	name = "member_tiers",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_member_tier_name", columnNames = {"cast_profile_id", "name_key"}),
		@UniqueConstraint(name = "uk_member_tier_rank", columnNames = {"cast_profile_id", "rank_number"})
	}
)
public class MemberTier {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cast_profile_id", nullable = false)
	private CastProfile castProfile;

	@Column(nullable = false, length = 80)
	private String name;

	@Column(name = "name_key", nullable = false, length = 100)
	private String nameKey;

	@Column(length = 500)
	private String description;

	@Column(name = "rank_number", nullable = false)
	private int rank;

	@Column(name = "monthly_price", nullable = false, precision = 10, scale = 2)
	private BigDecimal monthlyPrice;

	@Column(nullable = false, length = 3)
	private String currency;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MemberTier() {
	}

	public MemberTier(
		CastProfile castProfile,
		String name,
		String nameKey,
		String description,
		int rank,
		BigDecimal monthlyPrice,
		String currency
	) {
		this.castProfile = castProfile;
		this.name = name;
		this.nameKey = nameKey;
		this.description = description;
		this.rank = rank;
		this.monthlyPrice = monthlyPrice;
		this.currency = currency;
		this.active = true;
	}

	public void deactivate() {
		active = false;
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

	public CastProfile getCastProfile() {
		return castProfile;
	}

	public String getName() {
		return name;
	}

	public String getNameKey() {
		return nameKey;
	}

	public String getDescription() {
		return description;
	}

	public int getRank() {
		return rank;
	}

	public BigDecimal getMonthlyPrice() {
		return monthlyPrice;
	}

	public String getCurrency() {
		return currency;
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
