package com.br.real_estate_platform.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
		@Valid @NotNull Security security,
		@Valid @NotNull Cors cors,
		@Valid @NotNull Admin admin,
		@Valid @NotNull Storage storage) {

	public record Security(
			@NotBlank @Size(min = 32) String jwtSecret,
			@NotBlank String cookieName,
			boolean cookieSecure,
			@NotNull Duration sessionAbsoluteTtl,
			@NotNull Duration sessionIdleTimeout,
			@Min(1) int maxSessionsPerUser,
			@Min(1) int maxLoginAttempts,
			@NotNull Duration loginLockDuration,
			@NotBlank String mfaCookieName,
			@NotNull Duration mfaChallengeTtl,
			@NotBlank String mfaEncryptionKey,
			@NotBlank String mfaIssuer) {
	}

	public record Cors(List<String> allowedOrigins) {

		public Cors {
			allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
		}
	}

	public record Admin(String email, String name, String password) {

		public boolean isConfigured() {
			return StringUtils.hasText(email) && StringUtils.hasText(password);
		}
	}

	public record Storage(@NotBlank String photosDir) {
	}
}
