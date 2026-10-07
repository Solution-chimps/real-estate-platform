package com.br.real_estate_platform.exception;

public class MfaChallengeExpiredException extends RuntimeException {

	public MfaChallengeExpiredException() {
		super("A etapa de login expirou. Informe e-mail e senha novamente");
	}
}
