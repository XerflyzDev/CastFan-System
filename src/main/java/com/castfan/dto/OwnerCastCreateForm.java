package com.castfan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OwnerCastCreateForm {
	@NotBlank(message = "Display name is required")
	@Size(min = 2, max = 80, message = "Display name must contain 2 to 80 characters")
	private String displayName;

	@NotBlank(message = "Stage name is required")
	@Size(min = 2, max = 80, message = "Stage name must contain 2 to 80 characters")
	private String stageName;

	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }
	public String getStageName() { return stageName; }
	public void setStageName(String stageName) { this.stageName = stageName; }
}
