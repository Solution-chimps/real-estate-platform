package com.br.real_estate_platform.controller;

import com.br.real_estate_platform.dto.PhotoUploadResponse;
import com.br.real_estate_platform.service.PhotoStorageService;
import com.br.real_estate_platform.service.StoredPhoto;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class PhotoController {

	private final PhotoStorageService storage;

	@PostMapping(path = "/api/admin/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public PhotoUploadResponse upload(@RequestPart("file") MultipartFile file) {
		return storage.store(file);
	}

	// Filenames are random and never reused, so the response can be cached forever.
	@GetMapping("/api/photos/{filename}")
	public ResponseEntity<Resource> serve(@PathVariable String filename) {
		StoredPhoto photo = storage.load(filename);
		return ResponseEntity.ok()
				.contentType(photo.mediaType())
				.cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
				.headers(headers -> headers.setContentDisposition(ContentDisposition.inline().filename(filename).build()))
				.body(photo.resource());
	}
}
