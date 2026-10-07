package com.br.real_estate_platform.dto;

import com.br.real_estate_platform.validation.PhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ContactRequest(
		UUID propertyId,
		@NotBlank @Size(min = 3, max = 120) String name,
		@NotBlank @Size(max = 30) @PhoneNumber String phone,
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 10, max = 2000) String message) {
}
