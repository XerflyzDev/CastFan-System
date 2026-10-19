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
@Table(name = "store_event_images")
public class StoreEventImage {

	@Id
	@Column(name = "store_event_id")
	private Long storeEventId;

	@MapsId
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "store_event_id")
	private StoreEvent storeEvent;

	@Column(name = "content_type", nullable = false, length = 32)
	private String contentType;

	@Lob
	@Column(name = "image_data", nullable = false, columnDefinition = "MEDIUMBLOB")
	private byte[] imageData;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected StoreEventImage() {
	}

	public StoreEventImage(StoreEvent storeEvent, String contentType, byte[] imageData) {
		this.storeEvent = storeEvent;
		update(contentType, imageData);
	}

	public void update(String contentType, byte[] imageData) {
		this.contentType = contentType;
		this.imageData = imageData.clone();
	}

	@PrePersist
	@PreUpdate
	void updateTimestamp() {
		updatedAt = Instant.now();
	}

	public String getContentType() { return contentType; }
	public byte[] getImageData() { return imageData.clone(); }
}
