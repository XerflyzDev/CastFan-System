package com.castfan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OwnerCastPromotionForm {

	@NotNull(message = "Member is required")
	private Long memberProfileId;

	@NotBlank(message = "Stage name is required")
	@Size(max = 80, message = "Stage name must not exceed 80 characters")
	private String stageName;

	public Long getMemberProfileId() { return memberProfileId; }
	public void setMemberProfileId(Long memberProfileId) { this.memberProfileId = memberProfileId; }
	public String getStageName() { return stageName; }
	public void setStageName(String stageName) { this.stageName = stageName; }
}
