package com.br.real_estate_platform.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.br.real_estate_platform.exception.TooManyLoginAttemptsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginAttemptServiceTest {

	private static final Duration LOCK = Duration.ofMinutes(15);
	private static final String ACCOUNT = "Regina@ConstantinoSP.com.br";

	@Test
	void locksTheAccountAfterTheConfiguredNumberOfFailures() {
		LoginAttemptService service = new LoginAttemptService(2, LOCK, Clock.systemUTC());

		service.recordFailure(ACCOUNT);
		assertThatCode(() -> service.assertAllowed(ACCOUNT)).doesNotThrowAnyException();
		service.recordFailure(ACCOUNT.toLowerCase());

		assertThatThrownBy(() -> service.assertAllowed(ACCOUNT)).isInstanceOf(TooManyLoginAttemptsException.class);
	}

	@Test
	void forgetsFailuresOnceTheLockWindowHasPassed() {
		MutableClock clock = new MutableClock(Instant.parse("2026-10-07T12:00:00Z"));
		LoginAttemptService service = new LoginAttemptService(1, LOCK, clock);

		service.recordFailure(ACCOUNT);
		assertThatThrownBy(() -> service.assertAllowed(ACCOUNT)).isInstanceOf(TooManyLoginAttemptsException.class);

		clock.advance(LOCK.plusSeconds(1));
		assertThatCode(() -> service.assertAllowed(ACCOUNT)).doesNotThrowAnyException();
	}

	@Test
	void resetClearsTheHistoryAfterASuccessfulLogin() {
		LoginAttemptService service = new LoginAttemptService(1, LOCK, Clock.systemUTC());

		service.recordFailure(ACCOUNT);
		service.reset(ACCOUNT);

		assertThatCode(() -> service.assertAllowed(ACCOUNT)).doesNotThrowAnyException();
	}

	private static final class MutableClock extends Clock {

		private Instant now;

		private MutableClock(Instant now) {
			this.now = now;
		}

		private void advance(Duration duration) {
			now = now.plus(duration);
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return now;
		}
	}
}
