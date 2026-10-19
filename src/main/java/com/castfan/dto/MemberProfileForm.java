package com.castfan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MemberProfileForm {

	@NotBlank(message = "Display name is required")
	@Size(max = 80, message = "Display name must not exceed 80 characters")
	private String displayName;

	@Size(max = 500, message = "Bio must not exceed 500 characters")
	private String bio;

	private boolean removeImage;

	private boolean castUpdateNotifications;
	private boolean eventNotifications;

	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }
	public String getBio() { return bio; }
	public void setBio(String bio) { this.bio = bio; }
	public boolean isRemoveImage() { return removeImage; }
	public void setRemoveImage(boolean removeImage) { this.removeImage = removeImage; }
	public boolean isCastUpdateNotifications() { return castUpdateNotifications; }
	public void setCastUpdateNotifications(boolean castUpdateNotifications) { this.castUpdateNotifications = castUpdateNotifications; }
	public boolean isEventNotifications() { return eventNotifications; }
	public void setEventNotifications(boolean eventNotifications) { this.eventNotifications = eventNotifications; }
}
