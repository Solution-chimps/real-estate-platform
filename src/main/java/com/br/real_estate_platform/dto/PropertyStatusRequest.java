package com.br.real_estate_platform.dto;

import com.br.real_estate_platform.entity.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record PropertyStatusRequest(@NotNull PropertyStatus status) {
}
