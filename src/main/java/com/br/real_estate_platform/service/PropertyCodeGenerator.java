package com.br.real_estate_platform.service;

import com.br.real_estate_platform.entity.PropertyCodeCounter;
import com.br.real_estate_platform.repository.PropertyCodeCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PropertyCodeGenerator {

	private static final String CODE_FORMAT = "CST-%04d";

	private final PropertyCodeCounterRepository repository;

	// Runs inside the caller's transaction so the row lock is held until the property is saved.
	@Transactional(propagation = Propagation.MANDATORY)
	public String next() {
		PropertyCodeCounter counter = repository.findWithLockingById(PropertyCodeCounter.SINGLETON_ID)
				.orElseThrow(() -> new IllegalStateException("property_code_counter row is missing"));
		int value = counter.getNextValue();
		counter.setNextValue(value + 1);
		return format(value);
	}

	public static String format(int value) {
		return String.format(CODE_FORMAT, value);
	}
}
