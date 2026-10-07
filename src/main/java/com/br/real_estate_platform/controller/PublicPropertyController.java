package com.br.real_estate_platform.controller;

import com.br.real_estate_platform.dto.PageResponse;
import com.br.real_estate_platform.dto.PropertyFilter;
import com.br.real_estate_platform.dto.PropertyResponse;
import com.br.real_estate_platform.service.PropertyService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PublicPropertyController {

	private final PropertyService service;

	@GetMapping
	public PageResponse<PropertyResponse> list(@Valid PropertyFilter filter,
			@PageableDefault(sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return service.listPublished(filter, pageable);
	}

	@GetMapping("/neighborhoods")
	public List<String> neighborhoods() {
		return service.listPublishedNeighborhoods();
	}

	@GetMapping("/{id}")
	public PropertyResponse get(@PathVariable UUID id) {
		return service.getPublished(id);
	}
}
