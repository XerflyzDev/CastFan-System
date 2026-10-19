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
@Table(name = "store_promotion_images")
public class StorePromotionImage {
	@Id @Column(name = "store_promotion_id") private Long storePromotionId;
	@MapsId @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "store_promotion_id")
	private StorePromotion storePromotion;
	@Column(name = "content_type", nullable = false, length = 32) private String contentType;
	@Lob @Column(name = "image_data", nullable = false, columnDefinition = "MEDIUMBLOB") private byte[] imageData;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;

	protected StorePromotionImage() { }
	public StorePromotionImage(StorePromotion storePromotion, String contentType, byte[] imageData) {
		this.storePromotion = storePromotion;
		update(contentType, imageData);
	}
	public void update(String contentType, byte[] imageData) {
		this.contentType = contentType;
		this.imageData = imageData.clone();
	}
	@PrePersist @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
	public String getContentType() { return contentType; }
	public byte[] getImageData() { return imageData.clone(); }
}
