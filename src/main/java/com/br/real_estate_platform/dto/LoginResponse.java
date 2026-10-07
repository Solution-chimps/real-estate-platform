package com.br.real_estate_platform.dto;

public record LoginResponse(boolean mfaRequired, boolean enrollmentRequired) {
}
