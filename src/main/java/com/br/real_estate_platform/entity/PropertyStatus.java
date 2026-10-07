package com.br.real_estate_platform.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.EnumSet;
import java.util.Set;

public enum PropertyStatus {
	DRAFT("draft"),
	PUBLISHED("published"),
	ARCHIVED("archived");

	private final String value;

	PropertyStatus(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static PropertyStatus fromValue(String value) {
		for (PropertyStatus status : values()) {
			if (status.value.equals(value)) {
				return status;
			}
		}
		throw new IllegalArgumentException("Status inválido: " + value);
	}

	// An archived listing has to be restored as a draft and reviewed before it goes live again.
	public boolean canTransitionTo(PropertyStatus target) {
		return this == target || allowedTargets().contains(target);
	}

	private Set<PropertyStatus> allowedTargets() {
		return switch (this) {
			case DRAFT -> EnumSet.of(PUBLISHED, ARCHIVED);
			case PUBLISHED -> EnumSet.of(DRAFT, ARCHIVED);
			case ARCHIVED -> EnumSet.of(DRAFT);
		};
	}
}
