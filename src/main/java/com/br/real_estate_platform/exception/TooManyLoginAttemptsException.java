package com.br.real_estate_platform.exception;

public class TooManyLoginAttemptsException extends RuntimeException {

	public TooManyLoginAttemptsException() {
		super("Muitas tentativas de login. Aguarde alguns minutos e tente novamente");
	}
}
