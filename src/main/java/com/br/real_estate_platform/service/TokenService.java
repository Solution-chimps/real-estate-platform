package com.br.real_estate_platform.service;

import com.br.real_estate_platform.config.AppProperties;
import com.br.real_estate_platform.entity.AppUser;
import com.br.real_estate_platform.exception.MfaChallengeExpiredException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

	public static final String ISSUER = "real-estate-platform";
	public static final String ROLES_CLAIM = "roles";
	public static final String PURPOSE_CLAIM = "purpose";
	public static final String SESSION_PURPOSE = "session";
	public static final String MFA_CHALLENGE_PURPOSE = "mfa";
	public static final String MFA_CHALLENGE_DECODER = "mfaChallengeJwtDecoder";

	private static final String AMR_CLAIM = "amr";
	private static final List<String> SESSION_AMR = List.of("pwd", "otp");

	private final JwtEncoder encoder;
	private final JwtDecoder mfaChallengeDecoder;
	private final AppProperties.Security security;
	private final Clock clock;

	public TokenService(JwtEncoder encoder, @Qualifier(MFA_CHALLENGE_DECODER) JwtDecoder mfaChallengeDecoder,
			AppProperties properties, Clock clock) {
		this.encoder = encoder;
		this.mfaChallengeDecoder = mfaChallengeDecoder;
		this.security = properties.security();
		this.clock = clock;
	}

	public String issueSession(AppUser user) {
		return encode(baseClaims(user, security.tokenTtl())
				.claim(PURPOSE_CLAIM, SESSION_PURPOSE)
				.claim(AMR_CLAIM, SESSION_AMR)
				.claim(ROLES_CLAIM, List.of(user.getRole().name()))
				.claim("name", user.getName())
				.build());
	}

	// Proves the password step succeeded; it carries no roles and is rejected by the API.
	public String issueMfaChallenge(AppUser user) {
		return encode(baseClaims(user, security.mfaChallengeTtl())
				.claim(PURPOSE_CLAIM, MFA_CHALLENGE_PURPOSE)
				.build());
	}

	public String subjectOfMfaChallenge(String token) {
		try {
			return mfaChallengeDecoder.decode(token).getSubject();
		}
		catch (JwtException exception) {
			throw new MfaChallengeExpiredException();
		}
	}

	private JwtClaimsSet.Builder baseClaims(AppUser user, Duration ttl) {
		Instant now = clock.instant();
		return JwtClaimsSet.builder()
				.issuer(ISSUER)
				.issuedAt(now)
				.expiresAt(now.plus(ttl))
				.subject(user.getEmail());
	}

	private String encode(JwtClaimsSet claims) {
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
