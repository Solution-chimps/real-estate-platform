package com.br.real_estate_platform.service;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record StoredPhoto(Resource resource, MediaType mediaType) {
}
