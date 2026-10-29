package com.castfan.dto;

import jakarta.validation.constraints.NotNull;

public class OwnerCastAssignmentForm {

	@NotNull(message = "Cast is required")
	private Long castProfileId;

	public Long getCastProfileId() { return castProfileId; }
	public void setCastProfileId(Long castProfileId) { this.castProfileId = castProfileId; }
}
