package com.br.real_estate_platform.dto;

import com.br.real_estate_platform.entity.UserRole;

public record UserResponse(String name, String email, UserRole role) {
}
