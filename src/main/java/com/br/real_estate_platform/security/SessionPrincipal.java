package com.br.real_estate_platform.security;

import com.br.real_estate_platform.entity.UserRole;
import java.util.UUID;

public record SessionPrincipal(UUID userId, String email, String name, UserRole role) {
}
