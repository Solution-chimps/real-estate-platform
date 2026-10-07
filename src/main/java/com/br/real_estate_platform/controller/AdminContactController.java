package com.br.real_estate_platform.controller;

import com.br.real_estate_platform.dto.ContactResponse;
import com.br.real_estate_platform.dto.ContactSummaryResponse;
import com.br.real_estate_platform.dto.PageResponse;
import com.br.real_estate_platform.service.ContactService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/contacts")
@RequiredArgsConstructor
public class AdminContactController {

	private final ContactService service;

	@GetMapping
	public PageResponse<ContactResponse> list(
			@PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return service.listAll(pageable);
	}

	@GetMapping("/summary")
	public ContactSummaryResponse summary() {
		return service.summary();
	}

	@PatchMapping("/{id}/read")
	public ContactResponse markAsRead(@PathVariable UUID id) {
		return service.markAsRead(id);
	}
}
