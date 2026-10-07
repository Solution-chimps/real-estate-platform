package com.br.real_estate_platform.repository;

import com.br.real_estate_platform.dto.PropertyFilter;
import com.br.real_estate_platform.entity.Property;
import com.br.real_estate_platform.entity.PropertyStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PropertySpecifications {

	private static final char LIKE_ESCAPE = '\\';

	private PropertySpecifications() {
	}

	public static Specification<Property> hasStatus(PropertyStatus status) {
		return (root, query, builder) -> builder.equal(root.get("status"), status);
	}

	public static Specification<Property> matches(PropertyFilter filter) {
		return (root, query, builder) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (filter.purpose() != null) {
				predicates.add(builder.equal(root.get("purpose"), filter.purpose()));
			}
			if (filter.type() != null) {
				predicates.add(builder.equal(root.get("type"), filter.type()));
			}
			if (StringUtils.hasText(filter.neighborhood())) {
				predicates.add(builder.equal(root.get("neighborhood"), filter.neighborhood().trim()));
			}
			if (StringUtils.hasText(filter.query())) {
				predicates.add(textMatches(root, builder, filter.query()));
			}
			return builder.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static Predicate textMatches(Root<Property> root, CriteriaBuilder builder, String text) {
		String pattern = "%" + escapeLike(text.trim().toLowerCase(Locale.ROOT)) + "%";
		return builder.or(
				like(builder, root.get("title"), pattern),
				like(builder, root.get("neighborhood"), pattern),
				like(builder, root.get("code"), pattern),
				like(builder, root.get("description"), pattern));
	}

	private static Predicate like(CriteriaBuilder builder, Expression<String> field, String pattern) {
		return builder.like(builder.lower(field), pattern, LIKE_ESCAPE);
	}

	// The user text is a literal search, so SQL wildcards typed by the user must not expand.
	private static String escapeLike(String text) {
		return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}
}
