package com.castfan.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.castfan.entity.MemberPointTransaction;
import com.castfan.entity.MemberPointTransactionType;

public record MemberPointTransactionView(
	Long id,
	MemberPointTransactionType type,
	BigDecimal purchaseAmount,
	long pointsDelta,
	String receiptReference,
	String reason,
	Long originalTransactionId,
	String createdByDisplayName,
	Instant createdAt
) {
	public static MemberPointTransactionView from(MemberPointTransaction transaction) {
		return new MemberPointTransactionView(
			transaction.getId(),
			transaction.getType(),
			transaction.getPurchaseAmount(),
			transaction.getPointsDelta(),
			transaction.getReceiptReference(),
			transaction.getReason(),
			transaction.getOriginalTransaction() == null ? null : transaction.getOriginalTransaction().getId(),
			transaction.getCreatedBy().getDisplayName(),
			transaction.getCreatedAt()
		);
	}
}
