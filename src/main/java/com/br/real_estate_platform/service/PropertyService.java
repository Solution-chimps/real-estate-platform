package com.br.real_estate_platform.service;

import static com.br.real_estate_platform.repository.PropertySpecifications.hasStatus;
import static com.br.real_estate_platform.repository.PropertySpecifications.matches;

import com.br.real_estate_platform.dto.PageResponse;
import com.br.real_estate_platform.dto.PropertyFilter;
import com.br.real_estate_platform.dto.PropertyRequest;
import com.br.real_estate_platform.dto.PropertyResponse;
import com.br.real_estate_platform.entity.Property;
import com.br.real_estate_platform.entity.PropertyStatus;
import com.br.real_estate_platform.exception.InvalidStatusTransitionException;
import com.br.real_estate_platform.exception.ResourceNotFoundException;
import com.br.real_estate_platform.mapper.PropertyMapper;
import com.br.real_estate_platform.repository.PropertyRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PropertyService {

	private static final String NOT_FOUND_MESSAGE = "Imóvel não encontrado";

	private final PropertyRepository repository;
	private final PropertyMapper mapper;
	private final PropertyCodeGenerator codeGenerator;
	private final Clock clock;

	public PageResponse<PropertyResponse> listPublished(PropertyFilter filter, Pageable pageable) {
		Specification<Property> specification = hasStatus(PropertyStatus.PUBLISHED).and(matches(filter));
		return PageResponse.from(repository.findAll(specification, pageable).map(mapper::toResponse));
	}

	public PropertyResponse getPublished(UUID id) {
		return repository.findByIdAndStatus(id, PropertyStatus.PUBLISHED)
				.map(mapper::toResponse)
				.orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MESSAGE));
	}

	public List<String> listPublishedNeighborhoods() {
		return repository.findDistinctNeighborhoodsByStatus(PropertyStatus.PUBLISHED);
	}

	public PageResponse<PropertyResponse> listAll(Pageable pageable) {
		return PageResponse.from(repository.findAll(pageable).map(mapper::toResponse));
	}

	public PropertyResponse get(UUID id) {
		return mapper.toResponse(find(id));
	}

	@Transactional
	public PropertyResponse create(PropertyRequest request) {
		Property property = mapper.toEntity(request, codeGenerator.next(), clock.instant());
		return mapper.toResponse(repository.save(property));
	}

	@Transactional
	public PropertyResponse update(UUID id, PropertyRequest request) {
		Property property = find(id);
		if (!property.getStatus().canTransitionTo(request.status())) {
			throw new InvalidStatusTransitionException(property.getStatus(), request.status());
		}
		mapper.apply(property, request, clock.instant());
		return mapper.toResponse(property);
	}

	@Transactional
	public PropertyResponse changeStatus(UUID id, PropertyStatus target) {
		Property property = find(id);
		if (!property.getStatus().canTransitionTo(target)) {
			throw new InvalidStatusTransitionException(property.getStatus(), target);
		}
		property.setStatus(target);
		property.setUpdatedAt(clock.instant());
		return mapper.toResponse(property);
	}

	@Transactional
	public void delete(UUID id) {
		repository.delete(find(id));
	}

	private Property find(UUID id) {
		return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND_MESSAGE));
	}
}
