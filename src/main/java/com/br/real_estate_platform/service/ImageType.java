package com.br.real_estate_platform.service;

import java.util.Optional;
import org.springframework.http.MediaType;

public enum ImageType {
	JPEG("jpg", MediaType.IMAGE_JPEG),
	PNG("png", MediaType.IMAGE_PNG),
	WEBP("webp", MediaType.parseMediaType("image/webp"));

	private final String extension;
	private final MediaType mediaType;

	ImageType(String extension, MediaType mediaType) {
		this.extension = extension;
		this.mediaType = mediaType;
	}

	public String extension() {
		return extension;
	}

	public MediaType mediaType() {
		return mediaType;
	}

	public static Optional<ImageType> fromExtension(String extension) {
		for (ImageType type : values()) {
			if (type.extension.equals(extension)) {
				return Optional.of(type);
			}
		}
		return Optional.empty();
	}
}
