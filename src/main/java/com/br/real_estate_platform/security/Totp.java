package com.br.real_estate_platform.security;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// RFC 6238 time-based one-time passwords over the RFC 4226 HMAC-SHA1 construction, the
// profile every mainstream authenticator app implements.
public final class Totp {

	public static final int DIGITS = 6;
	public static final Duration STEP = Duration.ofSeconds(30);
	public static final int SECRET_LENGTH_BYTES = 20;

	private static final String ALGORITHM = "HmacSHA1";
	private static final int MODULUS = 1_000_000;

	private Totp() {
	}

	public static long stepAt(Instant instant) {
		return Math.floorDiv(instant.getEpochSecond(), STEP.getSeconds());
	}

	public static String codeAt(byte[] secret, long step) {
		byte[] hash = hmac(secret, ByteBuffer.allocate(Long.BYTES).putLong(step).array());
		int offset = hash[hash.length - 1] & 0x0F;
		int binary = ((hash[offset] & 0x7F) << 24)
				| ((hash[offset + 1] & 0xFF) << 16)
				| ((hash[offset + 2] & 0xFF) << 8)
				| (hash[offset + 3] & 0xFF);
		return String.format("%0" + DIGITS + "d", binary % MODULUS);
	}

	public static OptionalLong matchingStep(byte[] secret, String code, long currentStep, int window) {
		byte[] candidate = code.getBytes(StandardCharsets.US_ASCII);
		for (long step = currentStep - window; step <= currentStep + window; step++) {
			byte[] expected = codeAt(secret, step).getBytes(StandardCharsets.US_ASCII);
			if (MessageDigest.isEqual(expected, candidate)) {
				return OptionalLong.of(step);
			}
		}
		return OptionalLong.empty();
	}

	public static String otpauthUri(String issuer, String account, String base32Secret) {
		String encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8).replace("+", "%20");
		String encodedAccount = URLEncoder.encode(account, StandardCharsets.UTF_8).replace("+", "%20");
		return "otpauth://totp/" + encodedIssuer + ":" + encodedAccount
				+ "?secret=" + base32Secret
				+ "&issuer=" + encodedIssuer
				+ "&algorithm=SHA1&digits=" + DIGITS
				+ "&period=" + STEP.getSeconds();
	}

	private static byte[] hmac(byte[] secret, byte[] message) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(secret, ALGORITHM));
			return mac.doFinal(message);
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException("HMAC-SHA1 is unavailable", exception);
		}
	}
}
