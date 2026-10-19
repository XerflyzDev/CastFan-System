package com.castfan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CastProfileForm {

	@NotBlank(message = "Stage name is required")
	@Size(max = 80, message = "Stage name must not exceed 80 characters")
	private String stageName;

	@Size(max = 1000, message = "Bio must not exceed 1000 characters")
	private String bio;

	private boolean removeImage;

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

	public boolean isRemoveImage() { return removeImage; }
	public void setRemoveImage(boolean removeImage) { this.removeImage = removeImage; }

	public boolean isDiscoverable() {
		return discoverable;
	}

	public void setDiscoverable(boolean discoverable) {
		this.discoverable = discoverable;
	}
}
