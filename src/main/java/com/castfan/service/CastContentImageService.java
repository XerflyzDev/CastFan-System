package com.castfan.service;

import java.util.Optional;

import com.castfan.entity.CastContent;
import com.castfan.entity.CastContentImage;
import com.castfan.entity.ContentStatus;
import com.castfan.entity.UserStatus;
import com.castfan.repository.CastContentImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CastContentImageService {
	private final CastContentImageRepository repository;

	public CastContentImageService(CastContentImageRepository repository) { this.repository = repository; }

	@Transactional
	public void store(CastContent content, MultipartFile file) {
		var upload = ImageUploadSupport.read(file, "Content");
		if (upload == null) return;
		var existing = repository.findById(content.getId());
		var image = existing.orElseGet(() -> new CastContentImage(content, upload.contentType(), upload.data()));
		if (existing.isPresent()) image.update(upload.contentType(), upload.data());
		repository.save(image);
	}

	@Transactional public void remove(CastContent content) { repository.deleteById(content.getId()); }
	@Transactional(readOnly = true) public boolean hasImage(Long id) { return repository.existsById(id); }
	@Transactional(readOnly = true) public Optional<ImageData> loadOwned(String email, Long id) {
		return repository.findOwned(id, email).map(this::data);
	}
	@Transactional(readOnly = true) public Optional<ImageData> loadPublic(Long profileId, Long id) {
		return repository.findPublic(id, profileId, ContentStatus.PUBLISHED, UserStatus.ACTIVE).map(this::data);
	}
	@Transactional(readOnly = true) public String ownerUrl(CastContent content) {
		return repository.existsById(content.getId()) ? "/cast/content/" + content.getId() + "/image" : content.getImageUrl();
	}
	@Transactional(readOnly = true) public String publicUrl(CastContent content) {
		return repository.existsById(content.getId())
			? "/casts/" + content.getCastProfile().getId() + "/content/" + content.getId() + "/image"
			: content.getImageUrl();
	}
	private ImageData data(CastContentImage image) { return new ImageData(image.getContentType(), image.getImageData()); }
	public record ImageData(String contentType, byte[] imageData) { }
}
