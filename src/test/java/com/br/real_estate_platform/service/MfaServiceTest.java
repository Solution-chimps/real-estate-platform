package com.br.real_estate_platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.br.real_estate_platform.config.AppProperties;
import com.br.real_estate_platform.dto.MfaSetupResponse;
import com.br.real_estate_platform.entity.AppUser;
import com.br.real_estate_platform.exception.InvalidMfaCodeException;
import com.br.real_estate_platform.exception.MfaNotConfiguredException;
import com.br.real_estate_platform.exception.TooManyLoginAttemptsException;
import com.br.real_estate_platform.security.SecretEncryptor;
import com.br.real_estate_platform.security.Totp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MfaServiceTest {

	private static final String TEST_KEY = "dGVzdC1vbmx5LW1mYS1rZXktMzItYnl0ZXMtMDEyMzQ=";
	private static final int MAX_ATTEMPTS = 3;

	private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC);
	private SecretEncryptor encryptor;
	private MfaService service;
	private AppUser user;

	@BeforeEach
	void setUp() {
		AppProperties properties = new AppProperties(
				new AppProperties.Security("0123456789abcdef0123456789abcdef", Duration.ofHours(8), "session", false,
						MAX_ATTEMPTS, Duration.ofMinutes(15), "mfa", Duration.ofMinutes(5), TEST_KEY, "Constantino"),
				new AppProperties.Cors(List.of()),
				new AppProperties.Admin(null, null, null),
				new AppProperties.Storage("photos"));
		encryptor = new SecretEncryptor(properties);
		service = new MfaService(encryptor, new LoginAttemptService(properties, clock), clock, properties);
		user = new AppUser();
		user.setEmail("regina@constantinosp.com.br");
		user.setEnabled(true);
	}

	@Test
	void setupStoresTheSecretEncryptedAndExposesItOnlyOnce() {
		MfaSetupResponse response = service.setup(user);

		assertThat(user.getMfaSecret()).isNotBlank().doesNotContain(response.secret());
		assertThat(response.otpauthUri()).contains("secret=" + response.secret());
		assertThat(user.isMfaEnabled()).isFalse();
	}

	@Test
	void verifyEnablesMfaWithACurrentCodeAndRejectsReplay() {
		service.setup(user);
		String code = currentCode();

		service.verify(user, code);

		assertThat(user.isMfaEnabled()).isTrue();
		assertThat(user.getMfaLastUsedStep()).isEqualTo(Totp.stepAt(clock.instant()));
		assertThatThrownBy(() -> service.verify(user, code)).isInstanceOf(InvalidMfaCodeException.class);
	}

	@Test
	void verifyRejectsWrongCodesAndLocksAfterTooManyAttempts() {
		service.setup(user);
		String wrongCode = currentCode().equals("000000") ? "111111" : "000000";

		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			assertThatThrownBy(() -> service.verify(user, wrongCode)).isInstanceOf(InvalidMfaCodeException.class);
		}

		assertThatThrownBy(() -> service.verify(user, currentCode())).isInstanceOf(TooManyLoginAttemptsException.class);
	}

	@Test
	void verifyRequiresSetupFirst() {
		assertThatThrownBy(() -> service.verify(user, "123456")).isInstanceOf(MfaNotConfiguredException.class);
	}

	private String currentCode() {
		byte[] secret = encryptor.decrypt(user.getMfaSecret());
		return Totp.codeAt(secret, Totp.stepAt(clock.instant()));
	}
}
