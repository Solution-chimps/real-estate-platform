package com.br.real_estate_platform.service;

import com.br.real_estate_platform.config.AppProperties;
import com.br.real_estate_platform.dto.IssuedSession;
import com.br.real_estate_platform.entity.AppUser;
import com.br.real_estate_platform.entity.UserSession;
import com.br.real_estate_platform.repository.UserSessionRepository;
import com.br.real_estate_platform.security.SessionPrincipal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class SessionService {

	private static final int TOKEN_LENGTH_BYTES = 32;
	private static final int USER_AGENT_MAX_LENGTH = 255;
	// Sliding expiry is persisted at most once a minute per session to keep reads cheap.
	private static final Duration TOUCH_INTERVAL = Duration.ofMinutes(1);
	private static final Duration PURGE_RETENTION = Duration.ofDays(7);

	private final UserSessionRepository repository;
	private final AppProperties.Security security;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	public SessionService(UserSessionRepository repository, AppProperties properties, Clock clock) {
		this.repository = repository;
		this.security = properties.security();
		this.clock = clock;
	}

	@Transactional
	public IssuedSession open(AppUser user, String userAgent) {
		Instant now = clock.instant();
		enforceSessionLimit(user, now);
		byte[] raw = new byte[TOKEN_LENGTH_BYTES];
		random.nextBytes(raw);
		String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

		UserSession session = new UserSession();
		session.setId(UUID.randomUUID());
		session.setUser(user);
		session.setTokenHash(hash(token));
		session.setUserAgent(truncate(userAgent));
		session.setCreatedAt(now);
		session.setLastSeenAt(now);
		session.setExpiresAt(now.plus(security.sessionAbsoluteTtl()));
		repository.save(session);
		return new IssuedSession(token, session.getExpiresAt());
	}

	@Transactional
	public Optional<SessionPrincipal> authenticate(String token) {
		Instant now = clock.instant();
		return repository.findByTokenHash(hash(token))
				.filter(session -> session.isActiveAt(now, security.sessionIdleTimeout()))
				.filter(session -> session.getUser().isEnabled())
				.map(session -> {
					if (session.getLastSeenAt().plus(TOUCH_INTERVAL).isBefore(now)) {
						session.setLastSeenAt(now);
					}
					return toPrincipal(session.getUser());
				});
	}

	@Transactional
	public void touch(String token) {
		Instant now = clock.instant();
		repository.findByTokenHash(hash(token))
				.filter(session -> session.isActiveAt(now, security.sessionIdleTimeout()))
				.ifPresent(session -> session.setLastSeenAt(now));
	}

	@Transactional
	public void revoke(String token) {
		Instant now = clock.instant();
		repository.findByTokenHash(hash(token))
				.filter(session -> session.getRevokedAt() == null)
				.ifPresent(session -> session.setRevokedAt(now));
	}

	@Transactional
	public void revokeAllForUser(AppUser user) {
		repository.revokeAllForUser(user, clock.instant());
	}

	@Scheduled(cron = "0 0 3 * * *")
	@Transactional
	public void purgeStaleSessions() {
		int removed = repository.deleteStale(clock.instant().minus(PURGE_RETENTION));
		if (removed > 0) {
			log.info("Purged {} stale sessions", removed);
		}
	}

	public static String hash(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.US_ASCII)));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	// Oldest sessions give way so a stolen credential cannot pile up unlimited sessions.
	private void enforceSessionLimit(AppUser user, Instant now) {
		List<UserSession> active = repository
				.findByUserAndRevokedAtIsNullAndExpiresAtAfterOrderByLastSeenAtAsc(user, now);
		int excess = active.size() - security.maxSessionsPerUser() + 1;
		for (int index = 0; index < excess; index++) {
			active.get(index).setRevokedAt(now);
		}
	}

	private static SessionPrincipal toPrincipal(AppUser user) {
		return new SessionPrincipal(user.getId(), user.getEmail(), user.getName(), user.getRole());
	}

	private static String truncate(String userAgent) {
		if (userAgent == null) {
			return null;
		}
		return userAgent.length() <= USER_AGENT_MAX_LENGTH ? userAgent : userAgent.substring(0, USER_AGENT_MAX_LENGTH);
	}
}
