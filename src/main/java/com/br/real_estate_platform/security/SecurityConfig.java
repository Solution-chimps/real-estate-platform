package com.br.real_estate_platform.security;

import com.br.real_estate_platform.config.AppProperties;
import jakarta.servlet.DispatcherType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private static final String ADMIN_ROLE = "ADMIN";
	private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L;
	private static final String CSRF_HEADER = "X-XSRF-TOKEN";
	private static final String ARGON2_ID = "argon2";
	private static final String BCRYPT_ID = "bcrypt";
	private static final int ARGON2_SALT_LENGTH = 16;
	private static final int ARGON2_HASH_LENGTH = 32;
	private static final int ARGON2_PARALLELISM = 1;
	private static final int ARGON2_MEMORY_KIB = 19_456;
	private static final int ARGON2_ITERATIONS = 2;
	private static final int BCRYPT_STRENGTH = 12;

	@Bean
	public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, AppProperties properties,
			JwtDecoder jwtDecoder, JwtAuthenticationConverter jwtAuthenticationConverter,
			CookieBearerTokenResolver bearerTokenResolver, ProblemAuthenticationEntryPoint problemHandler,
			CorsConfigurationSource corsConfigurationSource) throws Exception {
		// The SPA never reads this cookie: it fetches a masked token from GET /api/auth/csrf and
		// echoes it in X-XSRF-TOKEN, so the cookie can stay HttpOnly. The default
		// XorCsrfTokenRequestAttributeHandler unmasks that header value on every request.
		CookieCsrfTokenRepository csrfRepository = new CookieCsrfTokenRepository();
		csrfRepository.setHeaderName(CSRF_HEADER);
		csrfRepository.setCookieCustomizer(cookie -> cookie
				.httpOnly(true)
				.secure(properties.security().cookieSecure())
				.sameSite("Strict")
				.path("/"));

		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.anonymous(Customizer.withDefaults())
				.headers(headers -> headers
						.frameOptions(frame -> frame.deny())
						.contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
						.httpStrictTransportSecurity(hsts -> hsts
								.includeSubDomains(true)
								.maxAgeInSeconds(HSTS_MAX_AGE_SECONDS)))
				.authorizeHttpRequests(auth -> auth
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/properties/**", "/api/photos/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/contacts").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout",
								"/api/auth/mfa/setup", "/api/auth/mfa/verify").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.requestMatchers("/api/admin/**").hasRole(ADMIN_ROLE)
						.requestMatchers("/api/auth/me").authenticated()
						.anyRequest().denyAll())
				.oauth2ResourceServer(oauth2 -> oauth2
						.bearerTokenResolver(bearerTokenResolver)
						.authenticationEntryPoint(problemHandler)
						.accessDeniedHandler(problemHandler)
						.jwt(jwt -> jwt
								.decoder(jwtDecoder)
								.jwtAuthenticationConverter(jwtAuthenticationConverter)))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(problemHandler)
						.accessDeniedHandler(problemHandler));
		return http.build();
	}

	// Argon2id with the OWASP minimum (19 MiB, 2 iterations, parallelism 1) is the default;
	// BCrypt at cost 12 stays registered so hashes can be migrated if the policy changes.
	@Bean
	public PasswordEncoder passwordEncoder() {
		Map<String, PasswordEncoder> encoders = new HashMap<>();
		encoders.put(ARGON2_ID, new Argon2PasswordEncoder(ARGON2_SALT_LENGTH, ARGON2_HASH_LENGTH,
				ARGON2_PARALLELISM, ARGON2_MEMORY_KIB, ARGON2_ITERATIONS));
		encoders.put(BCRYPT_ID, new BCryptPasswordEncoder(BCRYPT_STRENGTH));
		return new DelegatingPasswordEncoder(ARGON2_ID, encoders);
	}

	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource(AppProperties properties) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.cors().allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, CSRF_HEADER));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}
}
