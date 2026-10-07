package com.br.real_estate_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RealEstatePlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(RealEstatePlatformApplication.class, args);
	}

}
