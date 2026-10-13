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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "cast_follows",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_cast_follow_member_cast",
		columnNames = {"member_profile_id", "cast_profile_id"}
	)
)
public class CastFollow {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_profile_id", nullable = false)
	private MemberProfile memberProfile;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "cast_profile_id", nullable = false)
	private CastProfile castProfile;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected CastFollow() {
	}

	public CastFollow(MemberProfile memberProfile, CastProfile castProfile) {
		this.memberProfile = memberProfile;
		this.castProfile = castProfile;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public MemberProfile getMemberProfile() {
		return memberProfile;
	}

	public CastProfile getCastProfile() {
		return castProfile;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
