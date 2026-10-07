package com.br.real_estate_platform.dto;

import java.time.Instant;

public record IssuedSession(String token, Instant expiresAt) {
}
