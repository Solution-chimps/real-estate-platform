package com.br.real_estate_platform.service;

import com.br.real_estate_platform.dto.ContactRequest;
import com.br.real_estate_platform.dto.ContactResponse;
import com.br.real_estate_platform.dto.ContactSummaryResponse;
import com.br.real_estate_platform.dto.PageResponse;
import com.br.real_estate_platform.entity.Contact;
import com.br.real_estate_platform.entity.Property;
import com.br.real_estate_platform.entity.PropertyStatus;
import com.br.real_estate_platform.exception.ResourceNotFoundException;
import com.br.real_estate_platform.mapper.ContactMapper;
import com.br.real_estate_platform.repository.ContactRepository;
import com.br.real_estate_platform.repository.PropertyRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactService {

	private final ContactRepository repository;
	private final PropertyRepository propertyRepository;
	private final ContactMapper mapper;
	private final Clock clock;

	@Transactional
	public ContactResponse register(ContactRequest request) {
		Property property = null;
		if (request.propertyId() != null) {
			property = propertyRepository.findByIdAndStatus(request.propertyId(), PropertyStatus.PUBLISHED)
					.orElseThrow(() -> new ResourceNotFoundException("Imóvel não encontrado"));
		}
		Contact contact = mapper.toEntity(request, property, clock.instant());
		return mapper.toResponse(repository.save(contact));
	}

	public PageResponse<ContactResponse> listAll(Pageable pageable) {
		return PageResponse.from(repository.findAll(pageable).map(mapper::toResponse));
	}

	public ContactSummaryResponse summary() {
		return new ContactSummaryResponse(repository.countByReadFalse());
	}

	@Transactional
	public ContactResponse markAsRead(UUID id) {
		Contact contact = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Contato não encontrado"));
		contact.setRead(true);
		return mapper.toResponse(contact);
	}
}
