package com.br.real_estate_platform.exception;

public class InvalidMfaCodeException extends RuntimeException {

	public InvalidMfaCodeException() {
		super("Código de verificação inválido");
	}
}
