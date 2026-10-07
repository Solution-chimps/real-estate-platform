package com.br.real_estate_platform.dto;

import com.br.real_estate_platform.entity.PropertyPurpose;
import com.br.real_estate_platform.entity.PropertyStatus;
import com.br.real_estate_platform.entity.PropertyType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PropertyResponse(
		UUID id,
		String code,
		String title,
		String description,
		PropertyPurpose purpose,
		PropertyType type,
		String neighborhood,
		String city,
		BigDecimal price,
		BigDecimal condoFee,
		BigDecimal propertyTax,
		BigDecimal area,
		int bedrooms,
		int suites,
		int bathrooms,
		int parkingSpaces,
		List<String> features,
		List<String> photos,
		LocalDate availableFrom,
		boolean featured,
		PropertyStatus status,
		Instant createdAt,
		Instant updatedAt) {
}
