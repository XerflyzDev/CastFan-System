package com.castfan.entity;

import java.math.BigDecimal;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
	name = "member_point_transactions",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_member_point_receipt_key", columnNames = "receipt_reference_key"),
		@UniqueConstraint(name = "uk_member_point_original", columnNames = "original_transaction_id")
	},
	indexes = {
		@Index(name = "idx_member_point_member_created", columnList = "member_profile_id, created_at"),
		@Index(name = "idx_member_point_original", columnList = "original_transaction_id")
	}
)
public class MemberPointTransaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_profile_id", nullable = false)
	private MemberProfile memberProfile;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_user_id", nullable = false)
	private UserAccount createdBy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberPointTransactionType type;

	@Column(name = "purchase_amount", precision = 12, scale = 2)
	private BigDecimal purchaseAmount;

	@Column(name = "points_delta", nullable = false)
	private long pointsDelta;

	@Column(name = "receipt_reference", length = 120)
	private String receiptReference;

	@Column(name = "receipt_reference_key", length = 160)
	private String receiptReferenceKey;

	@Column(length = 500)
	private String reason;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "original_transaction_id")
	private MemberPointTransaction originalTransaction;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected MemberPointTransaction() {
	}

	public MemberPointTransaction(
		MemberProfile memberProfile,
		UserAccount createdBy,
		MemberPointTransactionType type,
		BigDecimal purchaseAmount,
		long pointsDelta,
		String receiptReference,
		String receiptReferenceKey,
		String reason,
		MemberPointTransaction originalTransaction
	) {
		this.memberProfile = memberProfile;
		this.createdBy = createdBy;
		this.type = type;
		this.purchaseAmount = purchaseAmount;
		this.pointsDelta = pointsDelta;
		this.receiptReference = receiptReference;
		this.receiptReferenceKey = receiptReferenceKey;
		this.reason = reason;
		this.originalTransaction = originalTransaction;
	}

	@PrePersist
	void onCreate() {
		createdAt = Instant.now();
	}

	public Long getId() { return id; }
	public MemberProfile getMemberProfile() { return memberProfile; }
	public UserAccount getCreatedBy() { return createdBy; }
	public MemberPointTransactionType getType() { return type; }
	public BigDecimal getPurchaseAmount() { return purchaseAmount; }
	public long getPointsDelta() { return pointsDelta; }
	public String getReceiptReference() { return receiptReference; }
	public String getReason() { return reason; }
	public MemberPointTransaction getOriginalTransaction() { return originalTransaction; }
	public Instant getCreatedAt() { return createdAt; }
}
