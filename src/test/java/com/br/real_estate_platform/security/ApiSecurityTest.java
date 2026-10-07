package com.br.real_estate_platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.real_estate_platform.config.AppProperties;
import com.br.real_estate_platform.dto.IssuedSession;
import com.br.real_estate_platform.entity.AppUser;
import com.br.real_estate_platform.entity.UserRole;
import com.br.real_estate_platform.entity.UserSession;
import com.br.real_estate_platform.repository.AppUserRepository;
import com.br.real_estate_platform.repository.UserSessionRepository;
import com.br.real_estate_platform.service.SessionService;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiSecurityTest {

	private static final String ADMIN_EMAIL = "regina@constantinosp.com.br";
	private static final String ADMIN_PASSWORD = "Senha-forte-para-teste-123";
	private static final String USER_AGENT = "JUnit";
	private static final String FRONT_ORIGIN = "https://localhost:4200";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppUserRepository users;

	@Autowired
	private UserSessionRepository userSessions;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private SessionService sessions;

	@Autowired
	private SecretEncryptor secretEncryptor;

	@Autowired
	private AppProperties properties;

	@Autowired
	private Clock clock;

	private AppUser admin;

	@BeforeEach
	void createAdmin() {
		userSessions.deleteAll();
		users.deleteAll();
		admin = new AppUser();
		admin.setId(UUID.randomUUID());
		admin.setEmail(ADMIN_EMAIL);
		admin.setName("Regina");
		admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
		admin.setRole(UserRole.ADMIN);
		admin.setEnabled(true);
		admin.setMfaEnabled(false);
		admin.setCreatedAt(clock.instant());
		users.save(admin);
	}

	@Test
	void publicListingIsOpenToEveryone() throws Exception {
		mockMvc.perform(get("/api/properties")).andExpect(status().isOk());
	}

	@Test
	void adminEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/admin/properties"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("Autenticação necessária"));
	}

	@Test
	void mutatingRequestWithoutOriginOrRefererIsRejected() throws Exception {
		mockMvc.perform(post("/api/contacts").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.detail").value("Acesso negado"));
	}

	@Test
	void mutatingRequestFromAnUnknownOriginIsRejectedEvenWithAValidSession() throws Exception {
		Cookie session = sessionCookie(sessions.open(admin, USER_AGENT));

		mockMvc.perform(post("/api/auth/alive").cookie(session).header(HttpHeaders.ORIGIN, "https://evil.example"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/auth/alive").cookie(session).header(HttpHeaders.ORIGIN, "null"))
				.andExpect(status().isForbidden());
	}

	@Test
	void mutatingRequestFromAnAllowedFrontendOriginPasses() throws Exception {
		mockMvc.perform(post("/api/contacts")
				.header(HttpHeaders.ORIGIN, FRONT_ORIGIN)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void mutatingRequestFromTheApiOwnOriginPasses() throws Exception {
		mockMvc.perform(post("/api/contacts")
				.header(HttpHeaders.ORIGIN, "http://localhost")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void refererIsAcceptedWhenTheBrowserOmitsOrigin() throws Exception {
		mockMvc.perform(post("/api/contacts")
				.header(HttpHeaders.REFERER, FRONT_ORIGIN + "/imoveis/abc")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void safeRequestsNeverRequireAnOrigin() throws Exception {
		mockMvc.perform(get("/api/properties")).andExpect(status().isOk());
	}

	@Test
	void wrongPasswordIsRejectedWithoutLeakingWhichPartFailed() throws Exception {
		mockMvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, FRONT_ORIGIN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson("senha-errada")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
	}

	@Test
	void passwordAloneOnlyYieldsAnMfaChallengeThatCannotReachTheApi() throws Exception {
		MvcResult login = mockMvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, FRONT_ORIGIN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(ADMIN_PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mfaRequired").value(true))
				.andExpect(jsonPath("$.enrollmentRequired").value(true))
				.andReturn();

		Cookie challenge = login.getResponse().getCookie(properties.security().mfaCookieName());
		assertThat(challenge).isNotNull();
		assertThat(challenge.isHttpOnly()).isTrue();
		assertThat(login.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict");
		assertThat(login.getResponse().getCookie(sessionCookieName())).isNull();

		mockMvc.perform(get("/api/admin/properties").cookie(new Cookie(sessionCookieName(), challenge.getValue())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void fullLoginEnrolsTheAuthenticatorAndOpensAnAdminSession() throws Exception {
		Cookie challenge = mockMvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, FRONT_ORIGIN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(ADMIN_PASSWORD)))
				.andReturn().getResponse().getCookie(properties.security().mfaCookieName());

		mockMvc.perform(post("/api/auth/mfa/setup").header(HttpHeaders.ORIGIN, FRONT_ORIGIN).cookie(challenge))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.secret").isNotEmpty())
				.andExpect(jsonPath("$.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")));

		String code = currentCodeFor(ADMIN_EMAIL);
		MvcResult verified = mockMvc.perform(post("/api/auth/mfa/verify").header(HttpHeaders.ORIGIN, FRONT_ORIGIN).cookie(challenge)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"" + code + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
				.andReturn();

		Cookie session = verified.getResponse().getCookie(sessionCookieName());
		assertThat(session).isNotNull();
		assertThat(session.isHttpOnly()).isTrue();
		assertThat(session.getValue()).hasSizeGreaterThanOrEqualTo(43);
		assertThat(userSessions.findByTokenHash(SessionService.hash(session.getValue()))).isPresent();

		mockMvc.perform(get("/api/admin/properties").cookie(session)).andExpect(status().isOk());
		mockMvc.perform(get("/api/auth/me").cookie(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void reusingTheSameTotpCodeIsRejected() throws Exception {
		Cookie challenge = mockMvc.perform(post("/api/auth/login").header(HttpHeaders.ORIGIN, FRONT_ORIGIN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(ADMIN_PASSWORD)))
				.andReturn().getResponse().getCookie(properties.security().mfaCookieName());
		mockMvc.perform(post("/api/auth/mfa/setup").header(HttpHeaders.ORIGIN, FRONT_ORIGIN).cookie(challenge)).andExpect(status().isOk());
		String code = currentCodeFor(ADMIN_EMAIL);
		String body = "{\"code\":\"" + code + "\"}";

		mockMvc.perform(post("/api/auth/mfa/verify").header(HttpHeaders.ORIGIN, FRONT_ORIGIN).cookie(challenge)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/mfa/verify").header(HttpHeaders.ORIGIN, FRONT_ORIGIN).cookie(challenge)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sessionOpenedByTheServiceGrantsAdminAccess() throws Exception {
		mockMvc.perform(get("/api/admin/contacts/summary").cookie(sessionCookie(sessions.open(admin, USER_AGENT))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unread").isNumber());
	}

	@Test
	void sessionIdleForLongerThanTheTimeoutIsRejectedAndTheCookieIsCleared() throws Exception {
		IssuedSession issued = sessions.open(admin, USER_AGENT);
		UserSession stored = storedSession(issued);
		stored.setLastSeenAt(clock.instant().minus(properties.security().sessionIdleTimeout()).minusSeconds(60));
		userSessions.save(stored);

		MvcResult rejected = mockMvc.perform(get("/api/admin/properties").cookie(sessionCookie(issued)))
				.andExpect(status().isUnauthorized())
				.andReturn();
		Cookie cleared = rejected.getResponse().getCookie(sessionCookieName());
		assertThat(cleared).isNotNull();
		assertThat(cleared.getMaxAge()).isZero();
	}

	@Test
	void sessionPastTheAbsoluteLimitIsRejectedEvenWhenActive() throws Exception {
		IssuedSession issued = sessions.open(admin, USER_AGENT);
		UserSession stored = storedSession(issued);
		stored.setExpiresAt(clock.instant().minusSeconds(1));
		userSessions.save(stored);

		mockMvc.perform(get("/api/admin/properties").cookie(sessionCookie(issued)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void keepAliveExtendsTheIdleWindow() throws Exception {
		IssuedSession issued = sessions.open(admin, USER_AGENT);
		UserSession stored = storedSession(issued);
		stored.setLastSeenAt(clock.instant().minus(Duration.ofHours(1)));
		userSessions.save(stored);

		mockMvc.perform(post("/api/auth/alive").header(HttpHeaders.ORIGIN, FRONT_ORIGIN).cookie(sessionCookie(issued)))
				.andExpect(status().isNoContent());

		assertThat(storedSession(issued).getLastSeenAt()).isAfter(clock.instant().minusSeconds(60));
	}

	@Test
	void logoutRevokesTheSessionOnTheServer() throws Exception {
		IssuedSession issued = sessions.open(admin, USER_AGENT);

		mockMvc.perform(post("/api/auth/logout").header(HttpHeaders.ORIGIN, FRONT_ORIGIN).cookie(sessionCookie(issued)))
				.andExpect(status().isNoContent());

		assertThat(storedSession(issued).getRevokedAt()).isNotNull();
		mockMvc.perform(get("/api/admin/properties").cookie(sessionCookie(issued)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void oldestSessionIsRevokedWhenTheUserExceedsTheLimit() throws Exception {
		IssuedSession oldest = sessions.open(admin, USER_AGENT);
		for (int index = 0; index < properties.security().maxSessionsPerUser(); index++) {
			sessions.open(admin, USER_AGENT);
		}

		assertThat(storedSession(oldest).getRevokedAt()).isNotNull();
		mockMvc.perform(get("/api/admin/properties").cookie(sessionCookie(oldest)))
				.andExpect(status().isUnauthorized());
	}

	private String sessionCookieName() {
		return properties.security().cookieName();
	}

	private Cookie sessionCookie(IssuedSession issued) {
		return new Cookie(sessionCookieName(), issued.token());
	}

	private UserSession storedSession(IssuedSession issued) {
		return userSessions.findByTokenHash(SessionService.hash(issued.token())).orElseThrow();
	}

	private String currentCodeFor(String email) {
		AppUser stored = users.findByEmailIgnoreCase(email).orElseThrow();
		byte[] secret = secretEncryptor.decrypt(stored.getMfaSecret());
		return Totp.codeAt(secret, Totp.stepAt(clock.instant()));
	}

	private static String loginJson(String password) {
		return "{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + password + "\"}";
	}
}
