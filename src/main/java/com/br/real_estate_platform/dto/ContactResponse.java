package com.br.real_estate_platform.dto;

import java.time.Instant;
import java.util.UUID;

public record ContactResponse(
		UUID id,
		UUID propertyId,
		String propertyTitle,
		String name,
		String phone,
		String email,
		String message,
		boolean read,
		Instant createdAt) {
}
