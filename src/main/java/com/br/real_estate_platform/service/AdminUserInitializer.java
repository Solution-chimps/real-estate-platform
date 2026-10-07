package com.br.real_estate_platform.service;

import com.br.real_estate_platform.config.AppProperties;
import com.br.real_estate_platform.entity.AppUser;
import com.br.real_estate_platform.entity.UserRole;
import com.br.real_estate_platform.repository.AppUserRepository;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class AdminUserInitializer implements ApplicationRunner {

	private final AppUserRepository repository;
	private final PasswordEncoder passwordEncoder;
	private final AppProperties properties;
	private final Clock clock;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (repository.count() > 0) {
			return;
		}
		AppProperties.Admin admin = properties.admin();
		if (!admin.isConfigured()) {
			log.warn("No user exists and APP_ADMIN_EMAIL/APP_ADMIN_PASSWORD are not set; the backoffice has no login");
			return;
		}
		AppUser user = new AppUser();
		user.setId(UUID.randomUUID());
		user.setEmail(admin.email().trim().toLowerCase(Locale.ROOT));
		user.setName(admin.name().trim());
		user.setPasswordHash(passwordEncoder.encode(admin.password()));
		user.setRole(UserRole.ADMIN);
		user.setEnabled(true);
		user.setCreatedAt(clock.instant());
		repository.save(user);
		log.info("Initial admin user created for {}", user.getEmail());
	}
}
