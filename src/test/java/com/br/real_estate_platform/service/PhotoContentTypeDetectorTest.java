package com.br.real_estate_platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class PhotoContentTypeDetectorTest {

	@Test
	void recognisesJpegPngAndWebpSignatures() {
		byte[] jpeg = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0 };
		byte[] png = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };
		byte[] webp = "RIFF\0\0\0\0WEBP".getBytes(StandardCharsets.US_ASCII);

		assertThat(PhotoContentTypeDetector.detect(jpeg)).contains(ImageType.JPEG);
		assertThat(PhotoContentTypeDetector.detect(png)).contains(ImageType.PNG);
		assertThat(PhotoContentTypeDetector.detect(webp)).contains(ImageType.WEBP);
	}

	@Test
	void rejectsAnythingElseIncludingRenamedFiles() {
		byte[] html = "<!doctype html>".getBytes(StandardCharsets.US_ASCII);
		byte[] riffWithoutWebp = "RIFF\0\0\0\0WAVE".getBytes(StandardCharsets.US_ASCII);
		byte[] tooShort = { (byte) 0xFF };

		assertThat(PhotoContentTypeDetector.detect(html)).isEmpty();
		assertThat(PhotoContentTypeDetector.detect(riffWithoutWebp)).isEmpty();
		assertThat(PhotoContentTypeDetector.detect(tooShort)).isEmpty();
	}
}
