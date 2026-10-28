package com.castfan.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public class CastShiftForm {

	@NotNull(message = "Shift date is required")
	private LocalDate date;

	@NotNull(message = "Start time is required")
	private LocalTime startsAt;

	@NotNull(message = "End time is required")
	private LocalTime endsAt;

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public LocalTime getStartsAt() {
		return startsAt;
	}

	public void setStartsAt(LocalTime startsAt) {
		this.startsAt = startsAt;
	}

	public LocalTime getEndsAt() {
		return endsAt;
	}

	public void setEndsAt(LocalTime endsAt) {
		this.endsAt = endsAt;
	}
}
