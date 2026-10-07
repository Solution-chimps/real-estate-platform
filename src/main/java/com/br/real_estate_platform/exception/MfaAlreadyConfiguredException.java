package com.br.real_estate_platform.exception;

public class MfaAlreadyConfiguredException extends RuntimeException {

	public MfaAlreadyConfiguredException() {
		super("O aplicativo autenticador já está configurado para esta conta");
	}
}
