package com.castfan.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OwnerEventForm {

	@NotNull(message = "Store is required")
	private Long storeId;

	@NotBlank(message = "Title is required")
	@Size(max = 160, message = "Title must not exceed 160 characters")
	private String title;

	@NotBlank(message = "Description is required")
	@Size(max = 2000, message = "Description must not exceed 2000 characters")
	private String description;

	@Size(max = 2048, message = "Image URL must not exceed 2048 characters")
	@Pattern(
		regexp = "^(?:https?://[^\\s]+|/images/[^\\s]+)?$",
		message = "Image URL must use http, https, or a local /images path"
	)
	private String imageUrl;

	@NotNull(message = "Event date is required")
	private LocalDate date;

	@NotNull(message = "Start time is required")
	private LocalTime startsAt;

	@NotNull(message = "End time is required")
	private LocalTime endsAt;

	public Long getStoreId() { return storeId; }
	public void setStoreId(Long storeId) { this.storeId = storeId; }
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getImageUrl() { return imageUrl; }
	public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
	public LocalDate getDate() { return date; }
	public void setDate(LocalDate date) { this.date = date; }
	public LocalTime getStartsAt() { return startsAt; }
	public void setStartsAt(LocalTime startsAt) { this.startsAt = startsAt; }
	public LocalTime getEndsAt() { return endsAt; }
	public void setEndsAt(LocalTime endsAt) { this.endsAt = endsAt; }
}
