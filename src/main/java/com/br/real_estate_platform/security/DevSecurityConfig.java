package com.br.real_estate_platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

// The H2 console only exists in the dev profile; it renders in frames and posts its own forms.
@Configuration
@Profile("dev")
public class DevSecurityConfig {

	@Bean
	@Order(0)
	public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
		http
				.securityMatcher("/h2-console/**")
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.csrf(AbstractHttpConfigurer::disable)
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
		return http.build();
	}
}
