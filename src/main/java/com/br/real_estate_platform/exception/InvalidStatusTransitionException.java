package com.br.real_estate_platform.exception;

import com.br.real_estate_platform.entity.PropertyStatus;

public class InvalidStatusTransitionException extends RuntimeException {

	public InvalidStatusTransitionException(PropertyStatus from, PropertyStatus to) {
		super("Não é possível mudar o status de " + from.getValue() + " para " + to.getValue());
	}
}
