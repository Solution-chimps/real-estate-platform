package com.br.real_estate_platform.security;

import com.br.real_estate_platform.config.AppProperties;
import com.br.real_estate_platform.service.TokenService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

@Configuration
public class JwtConfig {

	private static final String ROLE_PREFIX = "ROLE_";

	@Bean
	public SecretKey jwtSecretKey(AppProperties properties) {
		return new SecretKeySpec(properties.security().jwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
	}

	@Bean
	public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
	}

	// Only fully authenticated (password + TOTP) tokens may reach the API.
	@Bean
	@Primary
	public JwtDecoder sessionJwtDecoder(SecretKey jwtSecretKey) {
		return decoderFor(jwtSecretKey, TokenService.SESSION_PURPOSE);
	}

	@Bean(TokenService.MFA_CHALLENGE_DECODER)
	public JwtDecoder mfaChallengeJwtDecoder(SecretKey jwtSecretKey) {
		return decoderFor(jwtSecretKey, TokenService.MFA_CHALLENGE_PURPOSE);
	}

	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(TokenService.ROLES_CLAIM);
		authorities.setAuthorityPrefix(ROLE_PREFIX);
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	private static JwtDecoder decoderFor(SecretKey key, String purpose) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(),
				new JwtIssuerValidator(TokenService.ISSUER),
				purposeValidator(purpose)));
		return decoder;
	}

	private static OAuth2TokenValidator<Jwt> purposeValidator(String purpose) {
		OAuth2Error error = new OAuth2Error("invalid_token", "Token purpose mismatch", null);
		return jwt -> purpose.equals(jwt.getClaimAsString(TokenService.PURPOSE_CLAIM))
				? OAuth2TokenValidatorResult.success()
				: OAuth2TokenValidatorResult.failure(error);
	}
}
