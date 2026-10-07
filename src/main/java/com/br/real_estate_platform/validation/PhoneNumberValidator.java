package com.br.real_estate_platform.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

// Brazilian numbers: two-digit area code plus eight (landline) or nine (mobile) digits.
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

	private static final Set<Integer> VALID_DIGIT_COUNTS = Set.of(10, 11);

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}
		String digits = value.replaceAll("\\D", "");
		return VALID_DIGIT_COUNTS.contains(digits.length());
	}
}
