package com.br.real_estate_platform.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PropertyPurpose {
	SALE("sale"),
	RENT("rent"),
	SHORT_STAY("short-stay");

	private final String value;

	PropertyPurpose(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static PropertyPurpose fromValue(String value) {
		for (PropertyPurpose purpose : values()) {
			if (purpose.value.equals(value)) {
				return purpose;
			}
		}
		throw new IllegalArgumentException("Finalidade inválida: " + value);
	}
}
