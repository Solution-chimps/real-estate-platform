package com.br.real_estate_platform.dto;

import com.br.real_estate_platform.entity.PropertyPurpose;
import com.br.real_estate_platform.entity.PropertyStatus;
import com.br.real_estate_platform.entity.PropertyType;
import com.br.real_estate_platform.service.PhotoNaming;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PropertyRequest(
		@NotBlank @Size(min = 5, max = 160) String title,
		@NotBlank @Size(min = 20, max = 4000) String description,
		@NotNull PropertyPurpose purpose,
		@NotNull PropertyType type,
		@NotBlank @Size(max = 120) String neighborhood,
		@NotBlank @Size(max = 120) String city,
		@NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal price,
		@PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal condoFee,
		@PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal propertyTax,
		@NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal area,
		@NotNull @Min(0) @Max(99) Integer bedrooms,
		@NotNull @Min(0) @Max(99) Integer suites,
		@NotNull @Min(0) @Max(99) Integer bathrooms,
		@NotNull @Min(0) @Max(99) Integer parkingSpaces,
		@Size(max = 30) List<@NotBlank @Size(max = 80) String> features,
		@Size(max = 20) List<@NotBlank @Pattern(regexp = PhotoNaming.URL_PATTERN) String> photos,
		LocalDate availableFrom,
		boolean featured,
		@NotNull PropertyStatus status) {

	public PropertyRequest {
		features = features == null ? List.of() : List.copyOf(features);
		photos = photos == null ? List.of() : List.copyOf(photos);
	}
}
