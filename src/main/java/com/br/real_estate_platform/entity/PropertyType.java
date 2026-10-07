package com.br.real_estate_platform.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PropertyType {
	APARTMENT("apartment"),
	HOUSE("house"),
	COMMERCIAL("commercial"),
	LAND("land");

	private final String value;

	PropertyType(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static PropertyType fromValue(String value) {
		for (PropertyType type : values()) {
			if (type.value.equals(value)) {
				return type;
			}
		}
		throw new IllegalArgumentException("Tipo de imóvel inválido: " + value);
	}
}
