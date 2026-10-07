package com.br.real_estate_platform.mapper;

import com.br.real_estate_platform.dto.ContactRequest;
import com.br.real_estate_platform.dto.ContactResponse;
import com.br.real_estate_platform.entity.Contact;
import com.br.real_estate_platform.entity.Property;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ContactMapper {

	public ContactResponse toResponse(Contact contact) {
		Property property = contact.getProperty();
		return new ContactResponse(
				contact.getId(),
				property == null ? null : property.getId(),
				property == null ? null : property.getTitle(),
				contact.getName(),
				contact.getPhone(),
				contact.getEmail(),
				contact.getMessage(),
				contact.isRead(),
				contact.getCreatedAt());
	}

	public Contact toEntity(ContactRequest request, Property property, Instant now) {
		Contact contact = new Contact();
		contact.setId(UUID.randomUUID());
		contact.setProperty(property);
		contact.setName(request.name().trim());
		contact.setPhone(request.phone().trim());
		contact.setEmail(request.email().trim().toLowerCase());
		contact.setMessage(request.message().trim());
		contact.setRead(false);
		contact.setCreatedAt(now);
		return contact;
	}
}
