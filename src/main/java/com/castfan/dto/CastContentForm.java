package com.castfan.dto;

import com.castfan.entity.ContentAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CastContentForm {
	@NotBlank(message = "Title is required")
	@Size(max = 160, message = "Title must not exceed 160 characters")
	private String title;

	@NotBlank(message = "Content is required")
	@Size(max = 5000, message = "Content must not exceed 5000 characters")
	private String body;

	@Size(max = 2048, message = "Image URL must not exceed 2048 characters")
	@Pattern(regexp = "^(?:https?://[^\\s]+|/images/[^\\s]+)?$", message = "Image URL must use http, https, or /images")
	private String imageUrl;

	@NotNull(message = "Audience is required")
	private ContentAudience audience = ContentAudience.PUBLIC;

	private Long minimumTierId;

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getBody() { return body; }
	public void setBody(String body) { this.body = body; }
	public String getImageUrl() { return imageUrl; }
	public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
	public ContentAudience getAudience() { return audience; }
	public void setAudience(ContentAudience audience) { this.audience = audience; }
	public Long getMinimumTierId() { return minimumTierId; }
	public void setMinimumTierId(Long minimumTierId) { this.minimumTierId = minimumTierId; }
}
