package com.castfan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MemberProfileForm {

	@NotBlank(message = "Display name is required")
	@Size(max = 80, message = "Display name must not exceed 80 characters")
	private String displayName;

	@Size(max = 500, message = "Bio must not exceed 500 characters")
	private String bio;

	@Size(max = 2048, message = "Avatar URL must not exceed 2048 characters")
	@Pattern(
		regexp = "^(?:https?://[^\\s]+|/images/[^\\s]+)?$",
		message = "Avatar URL must use http, https, or a local /images path"
	)
	private String avatarUrl;

	private boolean castUpdateNotifications;
	private boolean eventNotifications;

	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }
	public String getBio() { return bio; }
	public void setBio(String bio) { this.bio = bio; }
	public String getAvatarUrl() { return avatarUrl; }
	public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
	public boolean isCastUpdateNotifications() { return castUpdateNotifications; }
	public void setCastUpdateNotifications(boolean castUpdateNotifications) { this.castUpdateNotifications = castUpdateNotifications; }
	public boolean isEventNotifications() { return eventNotifications; }
	public void setEventNotifications(boolean eventNotifications) { this.eventNotifications = eventNotifications; }
}
