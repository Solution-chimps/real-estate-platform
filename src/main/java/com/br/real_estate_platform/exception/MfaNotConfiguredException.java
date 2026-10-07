package com.br.real_estate_platform.exception;

public class MfaNotConfiguredException extends RuntimeException {

	public MfaNotConfiguredException() {
		super("Configure o aplicativo autenticador antes de informar o código");
	}
}
