package com.br.real_estate_platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Server-side session behind the opaque CFID cookie. Only the SHA-256 of the token is
// stored, so a database leak does not yield usable sessions.
@Entity
@Table(name = "user_session")
@Getter
@Setter
public class UserSession {

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 36, nullable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(name = "token_hash", nullable = false, length = 64, unique = true)
	private String tokenHash;

	@Column(name = "user_agent", length = 255)
	private String userAgent;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "last_seen_at", nullable = false)
	private Instant lastSeenAt;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "revoked_at")
	private Instant revokedAt;

	public boolean isActiveAt(Instant now, Duration idleTimeout) {
		return revokedAt == null
				&& now.isBefore(expiresAt)
				&& now.isBefore(lastSeenAt.plus(idleTimeout));
	}
}
