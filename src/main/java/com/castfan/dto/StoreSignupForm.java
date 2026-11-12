package com.castfan.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StoreSignupForm {
	@NotBlank @Size(min = 3, max = 80) private String ownerName;
	@NotBlank @Email @Size(max = 254) private String email;
	@NotBlank @Size(min = 8, max = 72) private String password;
	@NotBlank private String confirmPassword;
	@NotBlank @Size(min = 2, max = 120) private String storeName;
	@Size(max = 1000) private String description;
	@NotBlank @Size(max = 500) private String address;
	@NotBlank @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Use lowercase letters, numbers, and hyphens")
	@Size(min = 2, max = 100) private String slug;

	public String getOwnerName() { return ownerName; }
	public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
	public String getPassword() { return password; }
	public void setPassword(String password) { this.password = password; }
	public String getConfirmPassword() { return confirmPassword; }
	public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
	public String getStoreName() { return storeName; }
	public void setStoreName(String storeName) { this.storeName = storeName; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }
	public String getSlug() { return slug; }
	public void setSlug(String slug) { this.slug = slug; }
}
