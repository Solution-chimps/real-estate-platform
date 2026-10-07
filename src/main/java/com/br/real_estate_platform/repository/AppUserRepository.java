package com.br.real_estate_platform.repository;

import com.br.real_estate_platform.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, UUID> {

	Optional<AppUser> findByEmailIgnoreCase(String email);
}
