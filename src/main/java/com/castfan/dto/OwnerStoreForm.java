package com.castfan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OwnerStoreForm {

	@NotBlank(message = "Store name is required")
	@Size(max = 120, message = "Store name must not exceed 120 characters")
	private String name;

	@NotBlank(message = "Store slug is required")
	@Size(max = 100, message = "Store slug must not exceed 100 characters")
	@Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Use lowercase letters, numbers, and single hyphens")
	private String slug;

	@Size(max = 1000, message = "Description must not exceed 1000 characters")
	private String description;

	@Size(max = 500, message = "Address must not exceed 500 characters")
	private String address;

	@NotBlank(message = "Timezone is required")
	@Size(max = 64, message = "Timezone must not exceed 64 characters")
	private String timezone = "Asia/Bangkok";

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getSlug() { return slug; }
	public void setSlug(String slug) { this.slug = slug; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }
	public String getTimezone() { return timezone; }
	public void setTimezone(String timezone) { this.timezone = timezone; }
}
