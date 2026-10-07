package com.br.real_estate_platform.security;

// RFC 4648 Base32 without padding, the alphabet authenticator apps expect in otpauth URIs.
public final class Base32 {

	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
	private static final int BITS_PER_CHAR = 5;
	private static final int MASK = 0x1F;

	private Base32() {
	}

	public static String encode(byte[] data) {
		StringBuilder encoded = new StringBuilder((data.length * 8 + BITS_PER_CHAR - 1) / BITS_PER_CHAR);
		int buffer = 0;
		int bits = 0;
		for (byte value : data) {
			buffer = (buffer << 8) | (value & 0xFF);
			bits += 8;
			while (bits >= BITS_PER_CHAR) {
				encoded.append(ALPHABET.charAt((buffer >> (bits - BITS_PER_CHAR)) & MASK));
				bits -= BITS_PER_CHAR;
			}
		}
		if (bits > 0) {
			encoded.append(ALPHABET.charAt((buffer << (BITS_PER_CHAR - bits)) & MASK));
		}
		return encoded.toString();
	}
}
