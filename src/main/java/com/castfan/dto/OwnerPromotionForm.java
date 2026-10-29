package com.castfan.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OwnerPromotionForm {
	@NotBlank(message = "Title is required")
	@Size(max = 160, message = "Title must not exceed 160 characters")
	private String title;

	@NotBlank(message = "Description is required")
	@Size(max = 2000, message = "Description must not exceed 2000 characters")
	private String description;

	@Size(max = 64, message = "Code must not exceed 64 characters")
	@Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "Code may contain only letters, numbers, hyphens, and underscores")
	private String code;

	private boolean removeImage;

	@NotNull(message = "Start date is required")
	private LocalDate startsOn;

	@NotNull(message = "Start time is required")
	private LocalTime startsAt;

	@NotNull(message = "End date is required")
	private LocalDate endsOn;

	@NotNull(message = "End time is required")
	private LocalTime endsAt;

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public boolean isRemoveImage() { return removeImage; }
	public void setRemoveImage(boolean removeImage) { this.removeImage = removeImage; }
	public LocalDate getStartsOn() { return startsOn; }
	public void setStartsOn(LocalDate startsOn) { this.startsOn = startsOn; }
	public LocalTime getStartsAt() { return startsAt; }
	public void setStartsAt(LocalTime startsAt) { this.startsAt = startsAt; }
	public LocalDate getEndsOn() { return endsOn; }
	public void setEndsOn(LocalDate endsOn) { this.endsOn = endsOn; }
	public LocalTime getEndsAt() { return endsAt; }
	public void setEndsAt(LocalTime endsAt) { this.endsAt = endsAt; }
}
