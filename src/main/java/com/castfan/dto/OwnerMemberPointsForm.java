package com.castfan.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OwnerMemberPointsForm {

	@NotNull(message = "Enter the paid amount.")
	@DecimalMin(value = "0.01", message = "Paid amount must be greater than zero.")
	@Digits(integer = 10, fraction = 2, message = "Use up to 10 digits and 2 decimal places.")
	private BigDecimal paidAmount;

	@NotBlank(message = "Receipt reference is required.")
	@Size(max = 120, message = "Receipt reference must not exceed 120 characters.")
	private String receiptReference;

	private Long memberProfileId;

	public BigDecimal getPaidAmount() { return paidAmount; }
	public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
	public String getReceiptReference() { return receiptReference; }
	public void setReceiptReference(String receiptReference) { this.receiptReference = receiptReference; }
	public Long getMemberProfileId() { return memberProfileId; }
	public void setMemberProfileId(Long memberProfileId) { this.memberProfileId = memberProfileId; }
}
