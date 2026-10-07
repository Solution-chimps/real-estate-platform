package com.br.real_estate_platform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// CSRF defence without a token: browsers attach Origin (or at least Referer) to every
// state-changing request on their own and a page cannot forge them, so the API only
// has to confirm the request came from itself or from an allowed front-end origin.
// Together with SameSite=Strict cookies this keeps the client free of any CSRF code.
public final class OriginVerificationFilter extends OncePerRequestFilter {

	private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
	private static final String NULL_ORIGIN = "null";

	private final Set<String> allowedOrigins;
	private final ProblemAuthenticationEntryPoint problemHandler;

	public OriginVerificationFilter(Collection<String> allowedOrigins, ProblemAuthenticationEntryPoint problemHandler) {
		this.allowedOrigins = allowedOrigins.stream()
				.map(OriginVerificationFilter::normalize)
				.collect(Collectors.toUnmodifiableSet());
		this.problemHandler = problemHandler;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (SAFE_METHODS.contains(request.getMethod())) {
			chain.doFilter(request, response);
			return;
		}
		Optional<String> origin = requestOrigin(request);
		if (origin.isPresent() && isAllowed(origin.get(), request)) {
			chain.doFilter(request, response);
			return;
		}
		problemHandler.handle(request, response, new AccessDeniedException("Request origin not allowed"));
	}

	private boolean isAllowed(String origin, HttpServletRequest request) {
		return allowedOrigins.contains(origin) || origin.equals(ownOrigin(request));
	}

	private static Optional<String> requestOrigin(HttpServletRequest request) {
		String origin = request.getHeader(HttpHeaders.ORIGIN);
		if (StringUtils.hasText(origin)) {
			return NULL_ORIGIN.equals(origin) ? Optional.empty() : Optional.of(normalize(origin));
		}
		String referer = request.getHeader(HttpHeaders.REFERER);
		if (!StringUtils.hasText(referer)) {
			return Optional.empty();
		}
		try {
			URI uri = new URI(referer);
			if (uri.getScheme() == null || uri.getHost() == null) {
				return Optional.empty();
			}
			String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
			return Optional.of(normalize(uri.getScheme() + "://" + uri.getHost() + port));
		}
		catch (URISyntaxException exception) {
			return Optional.empty();
		}
	}

	private static String ownOrigin(HttpServletRequest request) {
		String origin = ServletUriComponentsBuilder.fromRequestUri(request)
				.replacePath(null)
				.replaceQuery(null)
				.build()
				.toUriString();
		return normalize(origin);
	}

	private static String normalize(String origin) {
		String trimmed = origin.trim().toLowerCase(Locale.ROOT);
		return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}
}
