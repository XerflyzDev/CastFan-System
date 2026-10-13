package com.castfan.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class MemberTierForm {

	@NotBlank(message = "Tier name is required")
	@Size(min = 2, max = 80, message = "Tier name must be between 2 and 80 characters")
	private String name;

	@Size(max = 500, message = "Description must not exceed 500 characters")
	private String description;

	@NotNull(message = "Rank is required")
	@Min(value = 1, message = "Rank must be at least 1")
	@Max(value = 100, message = "Rank must not exceed 100")
	private Integer rank;

	@NotNull(message = "Monthly price is required")
	@DecimalMin(value = "0.00", message = "Monthly price must not be negative")
	@Digits(integer = 8, fraction = 2, message = "Monthly price must have up to 8 digits and 2 decimal places")
	private BigDecimal monthlyPrice;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getRank() {
		return rank;
	}

	public void setRank(Integer rank) {
		this.rank = rank;
	}

	public BigDecimal getMonthlyPrice() {
		return monthlyPrice;
	}

	public void setMonthlyPrice(BigDecimal monthlyPrice) {
		this.monthlyPrice = monthlyPrice;
	}
}
