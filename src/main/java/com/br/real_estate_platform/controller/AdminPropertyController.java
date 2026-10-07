package com.br.real_estate_platform.controller;

import com.br.real_estate_platform.dto.PageResponse;
import com.br.real_estate_platform.dto.PropertyRequest;
import com.br.real_estate_platform.dto.PropertyResponse;
import com.br.real_estate_platform.dto.PropertyStatusRequest;
import com.br.real_estate_platform.service.PropertyService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/admin/properties")
@RequiredArgsConstructor
public class AdminPropertyController {

	private final PropertyService service;

	@GetMapping
	public PageResponse<PropertyResponse> list(
			@PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return service.listAll(pageable);
	}

	@GetMapping("/{id}")
	public PropertyResponse get(@PathVariable UUID id) {
		return service.get(id);
	}

	@PostMapping
	public ResponseEntity<PropertyResponse> create(@Valid @RequestBody PropertyRequest request) {
		PropertyResponse created = service.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").build(created.id());
		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	public PropertyResponse update(@PathVariable UUID id, @Valid @RequestBody PropertyRequest request) {
		return service.update(id, request);
	}

	@PatchMapping("/{id}/status")
	public PropertyResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody PropertyStatusRequest request) {
		return service.changeStatus(id, request.status());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}
}
