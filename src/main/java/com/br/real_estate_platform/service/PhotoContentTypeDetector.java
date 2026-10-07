package com.br.real_estate_platform.service;

import java.util.Arrays;
import java.util.Optional;

// The declared content type comes from the client and cannot be trusted; the file
// signature decides whether the upload is really an image.
public final class PhotoContentTypeDetector {

	public static final int HEADER_LENGTH = 12;

	private static final byte[] JPEG_SIGNATURE = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
	private static final byte[] PNG_SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };
	private static final byte[] RIFF_SIGNATURE = { 'R', 'I', 'F', 'F' };
	private static final byte[] WEBP_SIGNATURE = { 'W', 'E', 'B', 'P' };

	private PhotoContentTypeDetector() {
	}

	public static Optional<ImageType> detect(byte[] header) {
		if (startsWith(header, 0, JPEG_SIGNATURE)) {
			return Optional.of(ImageType.JPEG);
		}
		if (startsWith(header, 0, PNG_SIGNATURE)) {
			return Optional.of(ImageType.PNG);
		}
		if (startsWith(header, 0, RIFF_SIGNATURE) && startsWith(header, 8, WEBP_SIGNATURE)) {
			return Optional.of(ImageType.WEBP);
		}
		return Optional.empty();
	}

	private static boolean startsWith(byte[] header, int offset, byte[] signature) {
		if (header.length < offset + signature.length) {
			return false;
		}
		return Arrays.equals(header, offset, offset + signature.length, signature, 0, signature.length);
	}
}
