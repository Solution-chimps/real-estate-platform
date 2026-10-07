package com.br.real_estate_platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "property_code_counter")
@Getter
@Setter
public class PropertyCodeCounter {

	public static final int SINGLETON_ID = 1;

	@Id
	private Integer id;

	@Column(name = "next_value", nullable = false)
	private int nextValue;
}
