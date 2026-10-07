package com.br.real_estate_platform.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TotpTest {

	// Shared secret and timestamps from RFC 6238 Appendix B (SHA-1 column, last six digits).
	private static final byte[] RFC_SECRET = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

	@Test
	void producesTheReferenceCodesFromRfc6238() {
		assertThat(Totp.codeAt(RFC_SECRET, Totp.stepAt(Instant.ofEpochSecond(59)))).isEqualTo("287082");
		assertThat(Totp.codeAt(RFC_SECRET, Totp.stepAt(Instant.ofEpochSecond(1_111_111_109L)))).isEqualTo("081804");
		assertThat(Totp.codeAt(RFC_SECRET, Totp.stepAt(Instant.ofEpochSecond(1_234_567_890L)))).isEqualTo("005924");
	}

	@Test
	void acceptsCodesFromTheAdjacentStepsOnly() {
		long step = Totp.stepAt(Instant.ofEpochSecond(1_234_567_890L));

		assertThat(Totp.matchingStep(RFC_SECRET, Totp.codeAt(RFC_SECRET, step - 1), step, 1)).hasValue(step - 1);
		assertThat(Totp.matchingStep(RFC_SECRET, Totp.codeAt(RFC_SECRET, step + 1), step, 1)).hasValue(step + 1);
		assertThat(Totp.matchingStep(RFC_SECRET, Totp.codeAt(RFC_SECRET, step + 2), step, 1)).isEmpty();
	}

	@Test
	void buildsAnOtpauthUriThatAuthenticatorAppsUnderstand() {
		String uri = Totp.otpauthUri("Constantino Imoveis", "regina@constantinosp.com.br", "JBSWY3DPEHPK3PXP");

		assertThat(uri).startsWith("otpauth://totp/Constantino%20Imoveis:regina%40constantinosp.com.br?");
		assertThat(uri).contains("secret=JBSWY3DPEHPK3PXP", "issuer=Constantino%20Imoveis", "digits=6", "period=30");
	}

	@Test
	void base32EncodesWithoutPadding() {
		assertThat(Base32.encode("Hello!".getBytes(StandardCharsets.US_ASCII))).isEqualTo("JBSWY3DPEE");
	}
}
