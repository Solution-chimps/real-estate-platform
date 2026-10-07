package com.br.real_estate_platform.service;

import com.br.real_estate_platform.config.AppProperties;
import com.br.real_estate_platform.dto.MfaSetupResponse;
import com.br.real_estate_platform.entity.AppUser;
import com.br.real_estate_platform.exception.InvalidMfaCodeException;
import com.br.real_estate_platform.exception.MfaAlreadyConfiguredException;
import com.br.real_estate_platform.exception.MfaNotConfiguredException;
import com.br.real_estate_platform.security.Base32;
import com.br.real_estate_platform.security.SecretEncryptor;
import com.br.real_estate_platform.security.Totp;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.OptionalLong;
import org.springframework.stereotype.Service;

@Service
public class MfaService {

	// One step of tolerance each way absorbs clock drift between phone and server.
	private static final int VERIFICATION_WINDOW = 1;
	private static final String ATTEMPT_KEY_PREFIX = "mfa:";

	private final SecretEncryptor encryptor;
	private final LoginAttemptService attempts;
	private final Clock clock;
	private final String issuer;
	private final SecureRandom random = new SecureRandom();

	public MfaService(SecretEncryptor encryptor, LoginAttemptService attempts, Clock clock,
			AppProperties properties) {
		this.encryptor = encryptor;
		this.attempts = attempts;
		this.clock = clock;
		this.issuer = properties.security().mfaIssuer();
	}

	public MfaSetupResponse setup(AppUser user) {
		if (user.isMfaEnabled()) {
			throw new MfaAlreadyConfiguredException();
		}
		byte[] secret = new byte[Totp.SECRET_LENGTH_BYTES];
		random.nextBytes(secret);
		user.setMfaSecret(encryptor.encrypt(secret));
		user.setMfaLastUsedStep(null);
		String base32Secret = Base32.encode(secret);
		return new MfaSetupResponse(base32Secret, Totp.otpauthUri(issuer, user.getEmail(), base32Secret));
	}

	public void verify(AppUser user, String code) {
		String attemptKey = ATTEMPT_KEY_PREFIX + user.getEmail();
		attempts.assertAllowed(attemptKey);
		if (user.getMfaSecret() == null) {
			throw new MfaNotConfiguredException();
		}
		byte[] secret = encryptor.decrypt(user.getMfaSecret());
		OptionalLong matched = Totp.matchingStep(secret, code, Totp.stepAt(clock.instant()), VERIFICATION_WINDOW);
		if (matched.isEmpty() || isReplay(user, matched.getAsLong())) {
			attempts.recordFailure(attemptKey);
			throw new InvalidMfaCodeException();
		}
		user.setMfaLastUsedStep(matched.getAsLong());
		user.setMfaEnabled(true);
		attempts.reset(attemptKey);
	}

	private static boolean isReplay(AppUser user, long step) {
		return user.getMfaLastUsedStep() != null && step <= user.getMfaLastUsedStep();
	}
}
