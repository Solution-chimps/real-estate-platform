package com.br.real_estate_platform.security;

import com.br.real_estate_platform.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// Every cookie issued by the API is HttpOnly, Secure and SameSite=Strict; there is no
// servlet session, so no JSESSIONID exists. The browser is the only party that handles them.
@Component
public class SessionCookieFactory {

	private static final String COOKIE_PATH = "/api";

	private final AppProperties.Security security;

	public SessionCookieFactory(AppProperties properties) {
		this.security = properties.security();
	}

	public ResponseCookie session(String token) {
		return build(security.cookieName(), token, security.sessionAbsoluteTtl());
	}

	public ResponseCookie expiredSession() {
		return build(security.cookieName(), "", Duration.ZERO);
	}

	public ResponseCookie mfaChallenge(String token) {
		return build(security.mfaCookieName(), token, security.mfaChallengeTtl());
	}

	public ResponseCookie expiredMfaChallenge() {
		return build(security.mfaCookieName(), "", Duration.ZERO);
	}

	public Optional<String> readSession(HttpServletRequest request) {
		return read(request, security.cookieName());
	}

	public Optional<String> readMfaChallenge(HttpServletRequest request) {
		return read(request, security.mfaCookieName());
	}

	private static Optional<String> read(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
				return Optional.of(cookie.getValue());
			}
		}
		return Optional.empty();
	}

	private ResponseCookie build(String name, String value, Duration maxAge) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(security.cookieSecure())
				.sameSite("Strict")
				.path(COOKIE_PATH)
				.maxAge(maxAge)
				.build();
	}
}
