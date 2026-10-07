package com.br.real_estate_platform.dto;

import com.br.real_estate_platform.entity.PropertyPurpose;
import com.br.real_estate_platform.entity.PropertyType;
import jakarta.validation.constraints.Size;

public record PropertyFilter(
		PropertyPurpose purpose,
		PropertyType type,
		@Size(max = 120) String neighborhood,
		@Size(max = 120) String query) {
}
