package com.br.real_estate_platform.repository;

import com.br.real_estate_platform.entity.PropertyCodeCounter;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PropertyCodeCounterRepository extends JpaRepository<PropertyCodeCounter, Integer> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<PropertyCodeCounter> findWithLockingById(Integer id);
}
