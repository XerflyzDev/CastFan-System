package com.castfan.dto;

import com.castfan.entity.ContentAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CastContentForm {
	@Size(max = 160, message = "Title must not exceed 160 characters")
	private String title;

	@NotBlank(message = "Content is required")
	@Size(max = 5000, message = "Content must not exceed 5000 characters")
	private String body;

	private boolean removeImage;

	@NotNull(message = "Audience is required")
	private ContentAudience audience = ContentAudience.PUBLIC;

	private Integer minimumStoreTierRank;

	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }
	public String getBody() { return body; }
	public void setBody(String body) { this.body = body; }
	public boolean isRemoveImage() { return removeImage; }
	public void setRemoveImage(boolean removeImage) { this.removeImage = removeImage; }
	public ContentAudience getAudience() { return audience; }
	public void setAudience(ContentAudience audience) { this.audience = audience; }
	public Integer getMinimumStoreTierRank() { return minimumStoreTierRank; }
	public void setMinimumStoreTierRank(Integer minimumStoreTierRank) { this.minimumStoreTierRank = minimumStoreTierRank; }

	@Deprecated
	public Long getMinimumTierId() {
		return minimumStoreTierRank == null ? null : minimumStoreTierRank.longValue();
	}

	@Deprecated
	public void setMinimumTierId(Long minimumTierId) {
		this.minimumStoreTierRank = minimumTierId == null ? null : Math.toIntExact(minimumTierId);
	}
}
