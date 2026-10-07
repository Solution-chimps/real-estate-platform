package com.br.real_estate_platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "app_user")
@Getter
@Setter
public class AppUser {

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 36, nullable = false)
	private UUID id;

	@Column(nullable = false, length = 255, unique = true)
	private String email;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UserRole role;

	@Column(nullable = false)
	private boolean enabled;

	// AES-GCM ciphertext of the TOTP secret; see SecretEncryptor.
	@Column(name = "mfa_secret", length = 255)
	private String mfaSecret;

	@Column(name = "mfa_enabled", nullable = false)
	private boolean mfaEnabled;

	// Last accepted TOTP time step, so a captured code cannot be replayed inside its window.
	@Column(name = "mfa_last_used_step")
	private Long mfaLastUsedStep;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
