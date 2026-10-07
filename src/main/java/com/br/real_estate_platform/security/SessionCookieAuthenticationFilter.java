package com.br.real_estate_platform.security;

import com.br.real_estate_platform.service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

// Turns a valid CFID cookie into an authenticated principal. An invalid or expired cookie is
// cleared on the response so the browser stops sending it.
public final class SessionCookieAuthenticationFilter extends OncePerRequestFilter {

	private static final String ROLE_PREFIX = "ROLE_";

	private final SessionService sessions;
	private final SessionCookieFactory cookies;

	public SessionCookieAuthenticationFilter(SessionService sessions, SessionCookieFactory cookies) {
		this.sessions = sessions;
		this.cookies = cookies;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Optional<String> token = cookies.readSession(request);
		if (token.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
			Optional<SessionPrincipal> principal = sessions.authenticate(token.get());
			if (principal.isPresent()) {
				authenticate(request, principal.get());
			}
			else {
				response.addHeader(HttpHeaders.SET_COOKIE, cookies.expiredSession().toString());
			}
		}
		chain.doFilter(request, response);
	}

	private static void authenticate(HttpServletRequest request, SessionPrincipal principal) {
		UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
				principal, null, List.of(new SimpleGrantedAuthority(ROLE_PREFIX + principal.role().name())));
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
	}
}
