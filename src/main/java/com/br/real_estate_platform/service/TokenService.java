package com.br.real_estate_platform.service;

import com.br.real_estate_platform.config.AppProperties;
import com.br.real_estate_platform.entity.AppUser;
import com.br.real_estate_platform.exception.MfaChallengeExpiredException;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

// Issues the signed MFA challenge that proves the password step succeeded. It carries no
// roles, lives a few minutes and is only accepted by the /api/auth/mfa/* endpoints.
@Service
@RequiredArgsConstructor
public class TokenService {

	public static final String ISSUER = "real-estate-platform";
	public static final String PURPOSE_CLAIM = "purpose";
	public static final String MFA_CHALLENGE_PURPOSE = "mfa";

	private final JwtEncoder encoder;
	private final JwtDecoder mfaChallengeJwtDecoder;
	private final AppProperties properties;
	private final Clock clock;

	public String issueMfaChallenge(AppUser user) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.issuedAt(now)
				.expiresAt(now.plus(properties.security().mfaChallengeTtl()))
				.subject(user.getEmail())
				.claim(PURPOSE_CLAIM, MFA_CHALLENGE_PURPOSE)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public String subjectOfMfaChallenge(String token) {
		try {
			return mfaChallengeJwtDecoder.decode(token).getSubject();
		}
		catch (JwtException exception) {
			throw new MfaChallengeExpiredException();
		}
	}
}
