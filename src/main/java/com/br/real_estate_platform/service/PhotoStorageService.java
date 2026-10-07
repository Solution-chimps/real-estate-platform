package com.br.real_estate_platform.service;

import com.br.real_estate_platform.config.AppProperties;
import com.br.real_estate_platform.dto.PhotoUploadResponse;
import com.br.real_estate_platform.exception.PhotoStorageException;
import com.br.real_estate_platform.exception.ResourceNotFoundException;
import com.br.real_estate_platform.exception.UnsupportedPhotoException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoStorageService {

	private final Path root;

	public PhotoStorageService(AppProperties properties) {
		this.root = Path.of(properties.storage().photosDir()).toAbsolutePath().normalize();
	}

	public PhotoUploadResponse store(MultipartFile file) {
		ImageType type = PhotoContentTypeDetector.detect(readHeader(file)).orElseThrow(UnsupportedPhotoException::new);
		String filename = UUID.randomUUID() + "." + type.extension();
		Path target = resolveInsideRoot(filename);
		try (InputStream in = file.getInputStream()) {
			Files.createDirectories(root);
			Files.copy(in, target);
		}
		catch (IOException exception) {
			throw new PhotoStorageException("Could not write photo " + filename, exception);
		}
		return new PhotoUploadResponse(PhotoNaming.urlFor(filename));
	}

	public StoredPhoto load(String filename) {
		if (!PhotoNaming.isValidFilename(filename)) {
			throw new ResourceNotFoundException("Foto não encontrada");
		}
		Path path = resolveInsideRoot(filename);
		if (!Files.isRegularFile(path)) {
			throw new ResourceNotFoundException("Foto não encontrada");
		}
		String extension = filename.substring(filename.lastIndexOf('.') + 1);
		ImageType type = ImageType.fromExtension(extension)
				.orElseThrow(() -> new ResourceNotFoundException("Foto não encontrada"));
		return new StoredPhoto(new FileSystemResource(path), type.mediaType());
	}

	private byte[] readHeader(MultipartFile file) {
		try (InputStream in = file.getInputStream()) {
			return in.readNBytes(PhotoContentTypeDetector.HEADER_LENGTH);
		}
		catch (IOException exception) {
			throw new PhotoStorageException("Could not read uploaded photo", exception);
		}
	}

	// Filenames are generated or validated against a strict pattern, but the path is
	// still checked so a future caller cannot escape the storage directory.
	private Path resolveInsideRoot(String filename) {
		Path path = root.resolve(filename).normalize();
		if (!path.startsWith(root)) {
			throw new ResourceNotFoundException("Foto não encontrada");
		}
		return path;
	}
}
