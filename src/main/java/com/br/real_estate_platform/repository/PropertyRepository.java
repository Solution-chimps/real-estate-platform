package com.br.real_estate_platform.repository;

import com.br.real_estate_platform.entity.Property;
import com.br.real_estate_platform.entity.PropertyStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropertyRepository extends JpaRepository<Property, UUID>, JpaSpecificationExecutor<Property> {

	Optional<Property> findByIdAndStatus(UUID id, PropertyStatus status);

	@Query("select distinct p.neighborhood from Property p where p.status = :status order by p.neighborhood")
	List<String> findDistinctNeighborhoodsByStatus(@Param("status") PropertyStatus status);

	long countByStatus(PropertyStatus status);
}
