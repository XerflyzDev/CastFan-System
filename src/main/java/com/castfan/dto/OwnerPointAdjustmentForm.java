package com.castfan.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OwnerPointAdjustmentForm {
	@NotNull private Long memberProfileId;
	@NotNull private Long originalTransactionId;
	@NotNull(message = "Enter the corrected paid amount.")
	@DecimalMin(value = "0.01", message = "Corrected amount must be greater than zero.")
	@Digits(integer = 10, fraction = 2, message = "Use up to 10 digits and 2 decimal places.")
	private BigDecimal correctedPaidAmount;
	@NotBlank(message = "A correction reason is required.")
	@Size(max = 500, message = "Reason must not exceed 500 characters.")
	private String reason;

	public Long getMemberProfileId() { return memberProfileId; }
	public void setMemberProfileId(Long value) { memberProfileId = value; }
	public Long getOriginalTransactionId() { return originalTransactionId; }
	public void setOriginalTransactionId(Long value) { originalTransactionId = value; }
	public BigDecimal getCorrectedPaidAmount() { return correctedPaidAmount; }
	public void setCorrectedPaidAmount(BigDecimal value) { correctedPaidAmount = value; }
	public String getReason() { return reason; }
	public void setReason(String value) { reason = value; }
}
