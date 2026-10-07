package com.br.real_estate_platform.controller;

import com.br.real_estate_platform.dto.CsrfTokenResponse;
import com.br.real_estate_platform.dto.LoginRequest;
import com.br.real_estate_platform.dto.LoginResponse;
import com.br.real_estate_platform.dto.LoginResult;
import com.br.real_estate_platform.dto.MfaSetupResponse;
import com.br.real_estate_platform.dto.MfaVerifyRequest;
import com.br.real_estate_platform.dto.SessionResult;
import com.br.real_estate_platform.dto.UserResponse;
import com.br.real_estate_platform.exception.MfaChallengeExpiredException;
import com.br.real_estate_platform.security.SessionCookieFactory;
import com.br.real_estate_platform.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final SessionCookieFactory cookies;

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		LoginResult result = authService.login(request);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookies.mfaChallenge(result.challengeToken()).toString())
				.body(result.response());
	}

	@PostMapping("/mfa/setup")
	public MfaSetupResponse setupMfa(HttpServletRequest request) {
		return authService.setupMfa(challengeFrom(request));
	}

	@PostMapping("/mfa/verify")
	public ResponseEntity<UserResponse> verifyMfa(@Valid @RequestBody MfaVerifyRequest body,
			HttpServletRequest request) {
		SessionResult session = authService.verifyMfa(challengeFrom(request), body.code());
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, cookies.session(session.token()).toString())
				.header(HttpHeaders.SET_COOKIE, cookies.expiredMfaChallenge().toString())
				.body(session.user());
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, cookies.expiredSession().toString())
				.header(HttpHeaders.SET_COOKIE, cookies.expiredMfaChallenge().toString())
				.build();
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return authService.currentUser(jwt.getSubject());
	}

	// Reading the token materialises the XSRF-TOKEN cookie on this response and returns the
	// masked value the SPA must send back in the header named here.
	@GetMapping("/csrf")
	public CsrfTokenResponse csrf(CsrfToken csrfToken) {
		return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
	}

	private String challengeFrom(HttpServletRequest request) {
		return cookies.readMfaChallenge(request).orElseThrow(MfaChallengeExpiredException::new);
	}
}
