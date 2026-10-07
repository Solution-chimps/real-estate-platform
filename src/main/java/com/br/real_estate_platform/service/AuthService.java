package com.br.real_estate_platform.service;

import com.br.real_estate_platform.dto.LoginRequest;
import com.br.real_estate_platform.dto.LoginResponse;
import com.br.real_estate_platform.dto.LoginResult;
import com.br.real_estate_platform.dto.MfaSetupResponse;
import com.br.real_estate_platform.dto.SessionResult;
import com.br.real_estate_platform.dto.UserResponse;
import com.br.real_estate_platform.entity.AppUser;
import com.br.real_estate_platform.exception.MfaChallengeExpiredException;
import com.br.real_estate_platform.exception.ResourceNotFoundException;
import com.br.real_estate_platform.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final AppUserRepository userRepository;
	private final TokenService tokenService;
	private final LoginAttemptService loginAttempts;
	private final MfaService mfaService;

	// Step one of the login: the password alone never yields an API session, only a
	// short-lived challenge that the TOTP step exchanges for the real cookie.
	public LoginResult login(LoginRequest request) {
		String email = request.email().trim();
		loginAttempts.assertAllowed(email);
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
		}
		catch (AuthenticationException exception) {
			loginAttempts.recordFailure(email);
			throw exception;
		}
		AppUser user = findUser(email);
		loginAttempts.reset(email);
		LoginResponse response = new LoginResponse(true, !user.isMfaEnabled());
		return new LoginResult(tokenService.issueMfaChallenge(user), response);
	}

	@Transactional
	public MfaSetupResponse setupMfa(String challengeToken) {
		AppUser user = userFromChallenge(challengeToken);
		return mfaService.setup(user);
	}

	@Transactional
	public SessionResult verifyMfa(String challengeToken, String code) {
		AppUser user = userFromChallenge(challengeToken);
		mfaService.verify(user, code);
		return new SessionResult(tokenService.issueSession(user), toResponse(user));
	}

	public UserResponse currentUser(String email) {
		return userRepository.findByEmailIgnoreCase(email)
				.map(AuthService::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
	}

	private AppUser userFromChallenge(String challengeToken) {
		String email = tokenService.subjectOfMfaChallenge(challengeToken);
		return userRepository.findByEmailIgnoreCase(email)
				.filter(AppUser::isEnabled)
				.orElseThrow(MfaChallengeExpiredException::new);
	}

	private AppUser findUser(String email) {
		return userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new IllegalStateException("Authenticated user vanished: " + email));
	}

	private static UserResponse toResponse(AppUser user) {
		return new UserResponse(user.getName(), user.getEmail(), user.getRole());
	}
}
