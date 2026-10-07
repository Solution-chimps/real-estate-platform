package com.br.real_estate_platform.controller;

import com.br.real_estate_platform.dto.ContactRequest;
import com.br.real_estate_platform.dto.ContactResponse;
import com.br.real_estate_platform.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class PublicContactController {

	private final ContactService service;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ContactResponse register(@Valid @RequestBody ContactRequest request) {
		return service.register(request);
	}
}
