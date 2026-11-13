package com.castfan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StoreSettingsForm {
	@NotBlank @Size(min = 2, max = 120) private String name;
	@Size(max = 1000) private String description;
	@NotBlank @Size(max = 500) private String address;
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public String getAddress() { return address; }
	public void setAddress(String address) { this.address = address; }
}
