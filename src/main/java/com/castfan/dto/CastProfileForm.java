package com.castfan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CastProfileForm {

	@NotBlank(message = "Stage name is required")
	@Size(max = 80, message = "Stage name must not exceed 80 characters")
	private String stageName;

	@Size(max = 1000, message = "Bio must not exceed 1000 characters")
	private String bio;

	@Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
	@Pattern(
		regexp = "^(?:https?://[^\\s]+|/images/[^\\s]+)?$",
		message = "Avatar URL must use http, https, or a local /images path"
	)
	private String avatarUrl;

	private boolean discoverable;

	public String getStageName() {
		return stageName;
	}

	public void setStageName(String stageName) {
		this.stageName = stageName;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public boolean isDiscoverable() {
		return discoverable;
	}

	public void setDiscoverable(boolean discoverable) {
		this.discoverable = discoverable;
	}
}
