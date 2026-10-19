package com.castfan.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

final class ImageUploadSupport {

	static final int MAX_FILE_SIZE = 2 * 1024 * 1024;

	private ImageUploadSupport() { }

	static UploadedImage read(MultipartFile file, String subject) {
		if (file == null || file.isEmpty()) return null;
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new DomainRuleViolationException(subject + " image must not exceed 2 MB.");
		}
		try {
			var data = file.getBytes();
			return new UploadedImage(detectContentType(data, subject), data);
		} catch (IOException exception) {
			throw new DomainRuleViolationException("The selected " + subject.toLowerCase() + " image could not be read.");
		}
	}

	private static String detectContentType(byte[] data, String subject) {
		if (data.length >= 3 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8
			&& (data[2] & 0xff) == 0xff) return "image/jpeg";
		if (data.length >= 8 && (data[0] & 0xff) == 0x89 && data[1] == 0x50 && data[2] == 0x4e
			&& data[3] == 0x47 && data[4] == 0x0d && data[5] == 0x0a && data[6] == 0x1a && data[7] == 0x0a) {
			return "image/png";
		}
		if (data.length >= 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
			&& data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') return "image/webp";
		throw new DomainRuleViolationException("Use a JPG, PNG, or WebP " + subject.toLowerCase() + " image.");
	}

	record UploadedImage(String contentType, byte[] data) { }
}
