package com.br.real_estate_platform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.br.real_estate_platform.config.AppProperties;
import com.br.real_estate_platform.entity.AppUser;
import com.br.real_estate_platform.entity.UserRole;
import com.br.real_estate_platform.repository.AppUserRepository;
import com.br.real_estate_platform.service.TokenService;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// SecurityMockMvcRequestPostProcessors.csrf() swaps the CsrfFilter repository of the shared
// context for a test double, so the test that inspects the real repository must run first.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ApiSecurityTest {

	private static final String ADMIN_EMAIL = "regina@constantinosp.com.br";
	private static final String ADMIN_PASSWORD = "Senha-forte-para-teste-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AppUserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TokenService tokens;

	@Autowired
	private SecretEncryptor secretEncryptor;

	@Autowired
	private AppProperties properties;

	@Autowired
	private Clock clock;

	private AppUser admin;

	@BeforeEach
	void createAdmin() {
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
	void stateChangingRequestsWithoutCsrfTokenAreRejected() throws Exception {
		mockMvc.perform(post("/api/contacts")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@Order(1)
	void csrfEndpointIssuesAnHttpOnlyCookieAndAHeaderTokenThatPassesTheCheck() throws Exception {
		MvcResult primed = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andReturn();

		Cookie csrfCookie = primed.getResponse().getCookie("XSRF-TOKEN");
		assertThat(csrfCookie).isNotNull();
		assertThat(csrfCookie.isHttpOnly()).isTrue();
		String token = com.jayway.jsonpath.JsonPath.read(primed.getResponse().getContentAsString(), "$.token");

		mockMvc.perform(post("/api/contacts")
				.cookie(csrfCookie)
				.header("X-XSRF-TOKEN", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void wrongPasswordIsRejectedWithoutLeakingWhichPartFailed() throws Exception {
		mockMvc.perform(post("/api/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson("senha-errada")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
	}

	@Test
	void passwordAloneOnlyYieldsAnMfaChallengeThatCannotReachTheApi() throws Exception {
		MvcResult login = mockMvc.perform(post("/api/auth/login").with(csrf())
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
		assertThat(login.getResponse().getCookie(properties.security().cookieName())).isNull();

		mockMvc.perform(get("/api/admin/properties")
				.cookie(new Cookie(properties.security().cookieName(), challenge.getValue())))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void fullLoginEnrolsTheAuthenticatorAndOpensAnAdminSession() throws Exception {
		Cookie challenge = mockMvc.perform(post("/api/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(ADMIN_PASSWORD)))
				.andReturn().getResponse().getCookie(properties.security().mfaCookieName());

		mockMvc.perform(post("/api/auth/mfa/setup").with(csrf()).cookie(challenge))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.secret").isNotEmpty())
				.andExpect(jsonPath("$.otpauthUri").value(org.hamcrest.Matchers.startsWith("otpauth://totp/")));

		String code = currentCodeFor(ADMIN_EMAIL);
		MvcResult verified = mockMvc.perform(post("/api/auth/mfa/verify").with(csrf()).cookie(challenge)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\":\"" + code + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(ADMIN_EMAIL))
				.andReturn();

		Cookie session = verified.getResponse().getCookie(properties.security().cookieName());
		assertThat(session).isNotNull();
		assertThat(session.isHttpOnly()).isTrue();

		mockMvc.perform(get("/api/admin/properties").cookie(session)).andExpect(status().isOk());
		mockMvc.perform(get("/api/auth/me").cookie(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void reusingTheSameTotpCodeIsRejected() throws Exception {
		Cookie challenge = mockMvc.perform(post("/api/auth/login").with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginJson(ADMIN_PASSWORD)))
				.andReturn().getResponse().getCookie(properties.security().mfaCookieName());
		mockMvc.perform(post("/api/auth/mfa/setup").with(csrf()).cookie(challenge)).andExpect(status().isOk());
		String code = currentCodeFor(ADMIN_EMAIL);
		String body = "{\"code\":\"" + code + "\"}";

		mockMvc.perform(post("/api/auth/mfa/verify").with(csrf()).cookie(challenge)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/auth/mfa/verify").with(csrf()).cookie(challenge)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void sessionTokenIssuedDirectlyGrantsAdminAccess() throws Exception {
		Cookie session = new Cookie(properties.security().cookieName(), tokens.issueSession(admin));

		mockMvc.perform(get("/api/admin/contacts/summary").cookie(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unread").isNumber());
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
