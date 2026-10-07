package com.br.real_estate_platform.exception;

public class UnsupportedPhotoException extends RuntimeException {

	public UnsupportedPhotoException() {
		super("Envie uma imagem JPEG, PNG ou WebP");
	}
}
