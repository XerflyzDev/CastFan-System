package com.castfan.service;

import java.time.Instant;
import java.util.Optional;

import com.castfan.entity.StorePromotion;
import com.castfan.entity.StorePromotionImage;
import com.castfan.entity.PromotionStatus;
import com.castfan.repository.StorePromotionImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StorePromotionImageService {
	private final StorePromotionImageRepository repository;

	public StorePromotionImageService(StorePromotionImageRepository repository) { this.repository = repository; }

	@Transactional
	public void store(StorePromotion promotion, MultipartFile file) {
		var upload = ImageUploadSupport.read(file, "Promotion");
		if (upload == null) return;
		var existing = repository.findById(promotion.getId());
		var image = existing.orElseGet(() -> new StorePromotionImage(promotion, upload.contentType(), upload.data()));
		if (existing.isPresent()) image.update(upload.contentType(), upload.data());
		repository.save(image);
	}

	@Transactional public void remove(StorePromotion promotion) { repository.deleteById(promotion.getId()); }
	@Transactional(readOnly = true) public Optional<ImageData> loadOwned(String email, Long id) {
		return repository.findOwned(id, email).map(this::data);
	}
	@Transactional(readOnly = true) public Optional<ImageData> loadPublic(Long id) {
		return repository.findPublic(id, Instant.now(), PromotionStatus.ACTIVE).map(this::data);
	}
	@Transactional(readOnly = true) public String ownerUrl(StorePromotion promotion) {
		return repository.existsById(promotion.getId())
			? "/owner/promotions/" + promotion.getId() + "/image" : promotion.getBannerUrl();
	}
	@Transactional(readOnly = true) public String publicUrl(StorePromotion promotion) {
		return repository.existsById(promotion.getId())
			? "/promotions/" + promotion.getId() + "/image" : promotion.getBannerUrl();
	}
	private ImageData data(StorePromotionImage image) { return new ImageData(image.getContentType(), image.getImageData()); }
	public record ImageData(String contentType, byte[] imageData) { }
}
