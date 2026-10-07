package com.br.real_estate_platform.repository;

import com.br.real_estate_platform.entity.Contact;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

	@Override
	@EntityGraph(attributePaths = "property")
	Page<Contact> findAll(Pageable pageable);

	long countByReadFalse();
}
