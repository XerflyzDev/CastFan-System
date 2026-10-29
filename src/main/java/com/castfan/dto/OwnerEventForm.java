package com.castfan.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OwnerEventForm {

	@NotBlank(message = "Title is required")
	@Size(max = 160, message = "Title must not exceed 160 characters")
	private String title;

	@NotBlank(message = "Description is required")
	@Size(max = 2000, message = "Description must not exceed 2000 characters")
	private String description;

	private boolean removeImage;

	@NotNull(message = "Event date is required")
	private LocalDate date;

	@NotNull(message = "Start time is required")
	private LocalTime startsAt;

	@NotNull(message = "End time is required")
	private LocalTime endsAt;

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public boolean isRemoveImage() { return removeImage; }
	public void setRemoveImage(boolean removeImage) { this.removeImage = removeImage; }
	public LocalDate getDate() { return date; }
	public void setDate(LocalDate date) { this.date = date; }
	public LocalTime getStartsAt() { return startsAt; }
	public void setStartsAt(LocalTime startsAt) { this.startsAt = startsAt; }
	public LocalTime getEndsAt() { return endsAt; }
	public void setEndsAt(LocalTime endsAt) { this.endsAt = endsAt; }
}
