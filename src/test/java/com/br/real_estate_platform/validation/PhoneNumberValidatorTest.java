package com.br.real_estate_platform.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNumberValidatorTest {

	private final PhoneNumberValidator validator = new PhoneNumberValidator();

	@Test
	void acceptsMobileAndLandlineNumbersWithOrWithoutMask() {
		assertThat(validator.isValid("(11) 95062-0009", null)).isTrue();
		assertThat(validator.isValid("1142345678", null)).isTrue();
	}

	@Test
	void leavesEmptinessToTheNotBlankConstraint() {
		assertThat(validator.isValid(null, null)).isTrue();
		assertThat(validator.isValid("   ", null)).isTrue();
	}

	@Test
	void rejectsNumbersWithTooFewOrTooManyDigits() {
		assertThat(validator.isValid("95062-0009", null)).isFalse();
		assertThat(validator.isValid("+55 11 95062-00099", null)).isFalse();
	}
}
