package com.br.real_estate_platform.mapper;

import com.br.real_estate_platform.dto.PropertyRequest;
import com.br.real_estate_platform.dto.PropertyResponse;
import com.br.real_estate_platform.entity.Property;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PropertyMapper {

	public PropertyResponse toResponse(Property property) {
		return new PropertyResponse(
				property.getId(),
				property.getCode(),
				property.getTitle(),
				property.getDescription(),
				property.getPurpose(),
				property.getType(),
				property.getNeighborhood(),
				property.getCity(),
				property.getPrice(),
				property.getCondoFee(),
				property.getPropertyTax(),
				property.getArea(),
				property.getBedrooms(),
				property.getSuites(),
				property.getBathrooms(),
				property.getParkingSpaces(),
				List.copyOf(property.getFeatures()),
				List.copyOf(property.getPhotos()),
				property.getAvailableFrom(),
				property.isFeatured(),
				property.getStatus(),
				property.getCreatedAt(),
				property.getUpdatedAt());
	}

	public Property toEntity(PropertyRequest request, String code, Instant now) {
		Property property = new Property();
		property.setId(UUID.randomUUID());
		property.setCode(code);
		property.setCreatedAt(now);
		apply(property, request, now);
		return property;
	}

	public void apply(Property property, PropertyRequest request, Instant now) {
		property.setTitle(request.title().trim());
		property.setDescription(request.description().trim());
		property.setPurpose(request.purpose());
		property.setType(request.type());
		property.setNeighborhood(request.neighborhood().trim());
		property.setCity(request.city().trim());
		property.setPrice(request.price());
		property.setCondoFee(request.condoFee());
		property.setPropertyTax(request.propertyTax());
		property.setArea(request.area());
		property.setBedrooms(request.bedrooms());
		property.setSuites(request.suites());
		property.setBathrooms(request.bathrooms());
		property.setParkingSpaces(request.parkingSpaces());
		property.replaceFeatures(request.features().stream().map(String::trim).toList());
		property.replacePhotos(request.photos());
		property.setAvailableFrom(request.availableFrom());
		property.setFeatured(request.featured());
		property.setStatus(request.status());
		property.setUpdatedAt(now);
	}
}
