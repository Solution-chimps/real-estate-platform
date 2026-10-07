package com.br.real_estate_platform.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "property")
@Getter
@Setter
public class Property {

	// Listings are paginated, so the element collections cannot be fetch-joined without
	// in-memory paging; batch loading keeps a page at three queries instead of N+1.
	private static final int COLLECTION_BATCH_SIZE = 50;

	@Id
	@JdbcTypeCode(SqlTypes.CHAR)
	@Column(length = 36, nullable = false)
	private UUID id;

	@Column(nullable = false, length = 20, unique = true)
	private String code;

	@Column(nullable = false, length = 160)
	private String title;

	@Column(nullable = false, length = 4000)
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PropertyPurpose purpose;

	@Enumerated(EnumType.STRING)
	@Column(name = "property_type", nullable = false, length = 20)
	private PropertyType type;

	@Column(nullable = false, length = 120)
	private String neighborhood;

	@Column(nullable = false, length = 120)
	private String city;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal price;

	@Column(name = "condo_fee", precision = 14, scale = 2)
	private BigDecimal condoFee;

	@Column(name = "property_tax", precision = 14, scale = 2)
	private BigDecimal propertyTax;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal area;

	@Column(nullable = false)
	private int bedrooms;

	@Column(nullable = false)
	private int suites;

	@Column(nullable = false)
	private int bathrooms;

	@Column(name = "parking_spaces", nullable = false)
	private int parkingSpaces;

	@ElementCollection
	@CollectionTable(name = "property_feature", joinColumns = @JoinColumn(name = "property_id"))
	@OrderColumn(name = "position")
	@Column(name = "name", nullable = false, length = 80)
	@BatchSize(size = COLLECTION_BATCH_SIZE)
	private List<String> features = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "property_photo", joinColumns = @JoinColumn(name = "property_id"))
	@OrderColumn(name = "position")
	@Column(name = "url", nullable = false, length = 500)
	@BatchSize(size = COLLECTION_BATCH_SIZE)
	private List<String> photos = new ArrayList<>();

	@Column(name = "available_from")
	private LocalDate availableFrom;

	@Column(nullable = false)
	private boolean featured;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PropertyStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public void replaceFeatures(List<String> newFeatures) {
		features.clear();
		features.addAll(newFeatures);
	}

	public void replacePhotos(List<String> newPhotos) {
		photos.clear();
		photos.addAll(newPhotos);
	}
}
