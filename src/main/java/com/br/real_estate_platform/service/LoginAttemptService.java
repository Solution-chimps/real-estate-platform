package com.br.real_estate_platform.service;

import com.br.real_estate_platform.config.AppProperties;
import com.br.real_estate_platform.exception.TooManyLoginAttemptsException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

// In-memory throttle per account; good enough for a single instance and replaced by a
// shared store if the API ever runs on more than one node.
@Service
public class LoginAttemptService {

	private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();
	private final int maxAttempts;
	private final Duration lockDuration;
	private final Clock clock;

	// Two constructors exist (the package-private one serves unit tests); Spring needs the hint.
	@Autowired
	public LoginAttemptService(AppProperties properties, Clock clock) {
		this(properties.security().maxLoginAttempts(), properties.security().loginLockDuration(), clock);
	}

	LoginAttemptService(int maxAttempts, Duration lockDuration, Clock clock) {
		this.maxAttempts = maxAttempts;
		this.lockDuration = lockDuration;
		this.clock = clock;
	}

	public void assertAllowed(String account) {
		Deque<Instant> recent = failures.get(normalize(account));
		if (recent == null) {
			return;
		}
		synchronized (recent) {
			prune(recent);
			if (recent.size() >= maxAttempts) {
				throw new TooManyLoginAttemptsException();
			}
		}
	}

	public void recordFailure(String account) {
		Deque<Instant> recent = failures.computeIfAbsent(normalize(account), key -> new ArrayDeque<>());
		synchronized (recent) {
			prune(recent);
			recent.addLast(clock.instant());
		}
	}

	public void reset(String account) {
		failures.remove(normalize(account));
	}

	private void prune(Deque<Instant> recent) {
		Instant threshold = clock.instant().minus(lockDuration);
		while (!recent.isEmpty() && recent.peekFirst().isBefore(threshold)) {
			recent.pollFirst();
		}
	}

	private static String normalize(String account) {
		return account.trim().toLowerCase(Locale.ROOT);
	}
}
