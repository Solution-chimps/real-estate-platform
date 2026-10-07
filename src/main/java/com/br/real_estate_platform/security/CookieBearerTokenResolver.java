package com.br.real_estate_platform.security;

import com.br.real_estate_platform.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

// The JWT travels only in the HttpOnly session cookie; Authorization headers are ignored.
@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

	private final String cookieName;

	public CookieBearerTokenResolver(AppProperties properties) {
		this.cookieName = properties.security().cookieName();
	}

	@Override
	public String resolve(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookieName.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
