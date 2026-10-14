package com.castfan.dto;

import jakarta.validation.constraints.NotNull;

public class OwnerCastAssignmentForm {

	@NotNull(message = "Store is required")
	private Long storeId;

	@NotNull(message = "Cast is required")
	private Long castProfileId;

	public Long getStoreId() { return storeId; }
	public void setStoreId(Long storeId) { this.storeId = storeId; }
	public Long getCastProfileId() { return castProfileId; }
	public void setCastProfileId(Long castProfileId) { this.castProfileId = castProfileId; }
}
