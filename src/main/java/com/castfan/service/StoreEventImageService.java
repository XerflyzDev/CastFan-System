package com.castfan.service;

import java.io.IOException;
import java.util.Optional;

import com.castfan.entity.EventStatus;
import com.castfan.entity.StoreEvent;
import com.castfan.entity.StoreEventImage;
import com.castfan.repository.StoreEventImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StoreEventImageService {

	private static final int MAX_FILE_SIZE = 2 * 1024 * 1024;

	private final StoreEventImageRepository storeEventImageRepository;

	public StoreEventImageService(StoreEventImageRepository storeEventImageRepository) {
		this.storeEventImageRepository = storeEventImageRepository;
	}

	@Transactional
	public void store(StoreEvent event, MultipartFile file) {
		if (file == null || file.isEmpty()) return;
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new DomainRuleViolationException("Event image must not exceed 2 MB.");
		}

		byte[] imageData;
		try {
			imageData = file.getBytes();
		} catch (IOException exception) {
			throw new DomainRuleViolationException("The selected event image could not be read.");
		}

		var contentType = detectContentType(imageData);
		var existing = storeEventImageRepository.findById(event.getId());
		var image = existing.orElseGet(() -> new StoreEventImage(event, contentType, imageData));
		if (existing.isPresent()) image.update(contentType, imageData);
		storeEventImageRepository.save(image);
	}

	@Transactional
	public void remove(StoreEvent event) {
		storeEventImageRepository.deleteById(event.getId());
	}

	@Transactional(readOnly = true)
	public Optional<ImageData> loadOwned(String email, Long eventId) {
		return storeEventImageRepository.findOwnedImage(eventId, email).map(this::toImageData);
	}

	@Transactional(readOnly = true)
	public Optional<ImageData> loadPublic(Long eventId) {
		return storeEventImageRepository.findPublicImage(eventId, EventStatus.PUBLISHED).map(this::toImageData);
	}

	@Transactional(readOnly = true)
	public String ownerImageUrl(StoreEvent event) {
		return storeEventImageRepository.existsById(event.getId())
			? "/owner/events/" + event.getId() + "/image"
			: event.getImageUrl();
	}

	@Transactional(readOnly = true)
	public String publicImageUrl(StoreEvent event) {
		return storeEventImageRepository.existsById(event.getId())
			? "/events/" + event.getId() + "/image"
			: event.getImageUrl();
	}

	private ImageData toImageData(StoreEventImage image) {
		return new ImageData(image.getContentType(), image.getImageData());
	}

	private String detectContentType(byte[] data) {
		if (data.length >= 3 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8
			&& (data[2] & 0xff) == 0xff) return "image/jpeg";
		if (data.length >= 8 && (data[0] & 0xff) == 0x89 && data[1] == 0x50 && data[2] == 0x4e
			&& data[3] == 0x47 && data[4] == 0x0d && data[5] == 0x0a && data[6] == 0x1a && data[7] == 0x0a) {
			return "image/png";
		}
		if (data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
			&& data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') return "image/webp";
		throw new DomainRuleViolationException("Use a JPG, PNG, or WebP event image.");
	}

	public record ImageData(String contentType, byte[] imageData) { }
}
