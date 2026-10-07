package com.br.real_estate_platform.config;

import com.br.real_estate_platform.entity.PropertyPurpose;
import com.br.real_estate_platform.entity.PropertyStatus;
import com.br.real_estate_platform.entity.PropertyType;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	// Query parameters arrive with the public JSON value ("short-stay"), not the enum name.
	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addConverter(String.class, PropertyPurpose.class, PropertyPurpose::fromValue);
		registry.addConverter(String.class, PropertyType.class, PropertyType::fromValue);
		registry.addConverter(String.class, PropertyStatus.class, PropertyStatus::fromValue);
	}
}
