package com.br.real_estate_platform.security;

import com.br.real_estate_platform.config.AppProperties;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

// AES-256-GCM for secrets that must be recoverable (TOTP seeds). A random nonce is
// prepended to every ciphertext; the stored value is base64(nonce || ciphertext || tag).
@Component
public class SecretEncryptor {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int KEY_LENGTH_BYTES = 32;
	private static final int NONCE_LENGTH_BYTES = 12;
	private static final int TAG_LENGTH_BITS = 128;

	private final SecretKey key;
	private final SecureRandom random = new SecureRandom();

	public SecretEncryptor(AppProperties properties) {
		byte[] raw = Base64.getDecoder().decode(properties.security().mfaEncryptionKey());
		if (raw.length != KEY_LENGTH_BYTES) {
			throw new IllegalStateException("APP_MFA_ENCRYPTION_KEY must be the base64 of exactly 32 bytes");
		}
		this.key = new SecretKeySpec(raw, "AES");
	}

	public String encrypt(byte[] plaintext) {
		byte[] nonce = new byte[NONCE_LENGTH_BYTES];
		random.nextBytes(nonce);
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
			byte[] ciphertext = cipher.doFinal(plaintext);
			ByteBuffer packed = ByteBuffer.allocate(nonce.length + ciphertext.length).put(nonce).put(ciphertext);
			return Base64.getEncoder().encodeToString(packed.array());
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Could not encrypt secret", exception);
		}
	}

	public byte[] decrypt(String encoded) {
		byte[] packed = Base64.getDecoder().decode(encoded);
		if (packed.length <= NONCE_LENGTH_BYTES) {
			throw new IllegalStateException("Stored secret is truncated");
		}
		byte[] nonce = new byte[NONCE_LENGTH_BYTES];
		byte[] ciphertext = new byte[packed.length - NONCE_LENGTH_BYTES];
		ByteBuffer.wrap(packed).get(nonce).get(ciphertext);
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
			return cipher.doFinal(ciphertext);
		}
		catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Could not decrypt secret", exception);
		}
	}
}
