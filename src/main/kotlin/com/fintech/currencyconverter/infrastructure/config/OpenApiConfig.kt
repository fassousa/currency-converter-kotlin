package com.fintech.currencyconverter.infrastructure.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Currency Converter API")
                    .description("REST API for currency conversion built with Kotlin/Spring Boot")
                    .version("1.0.0"),
            )
            .servers(listOf(
                Server().url("http://localhost:8080/api/v1").description("Development server"),
                Server().url("https://kotlin-converter.duckdns.org/api/v1").description("Production server"),
            ))
            .components(
                Components().addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token obtained from POST /auth/sign_in"),
                ),
            )
}

