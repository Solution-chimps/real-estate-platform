package com.br.real_estate_platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	public static final String SESSION_COOKIE_SCHEME = "sessionCookie";

	@Bean
	public OpenAPI openApi(AppProperties properties) {
		return new OpenAPI()
				.info(new Info()
						.title("Constantino Imoveis API")
						.version("v1")
						.description("API do portal e do backoffice da Constantino Imoveis & Patrimonio"))
				.components(new Components().addSecuritySchemes(SESSION_COOKIE_SCHEME, new SecurityScheme()
						.type(SecurityScheme.Type.APIKEY)
						.in(SecurityScheme.In.COOKIE)
						.name(properties.security().cookieName())));
	}
}
