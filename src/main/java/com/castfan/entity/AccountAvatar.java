package com.castfan.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_avatars")
public class AccountAvatar {

	@Id
	@Column(name = "user_account_id")
	private Long userAccountId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_account_id")
	private UserAccount userAccount;

	@Column(name = "content_type", nullable = false, length = 32)
	private String contentType;

	@Lob
	@Column(name = "image_data", nullable = false, columnDefinition = "MEDIUMBLOB")
	private byte[] imageData;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected AccountAvatar() {
	}

	public AccountAvatar(UserAccount userAccount, String contentType, byte[] imageData) {
		this.userAccount = userAccount;
		update(contentType, imageData);
	}

	@PrePersist
	@PreUpdate
	void updateTimestamp() {
		updatedAt = Instant.now();
	}

	public void update(String contentType, byte[] imageData) {
		this.contentType = contentType;
		this.imageData = imageData.clone();
	}

	public String getContentType() {
		return contentType;
	}

	public byte[] getImageData() {
		return imageData.clone();
	}
}
