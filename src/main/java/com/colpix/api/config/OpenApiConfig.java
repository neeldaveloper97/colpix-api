package com.colpix.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearer-token";

    @Bean
    OpenAPI colpixOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Colpix Products API")
                        .version("v1")
                        .description("""
                                A RESTful API gateway that exposes a curated, authenticated Products
                                interface on top of the public restful-api.dev backend. Authentication
                                is performed locally via client_id / client_secret in exchange for a
                                short-lived bearer token.
                                """)
                        .contact(new Contact().name("Colpix Engineering"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("opaque")
                                .description("Bearer token obtained from POST /api/v1/auth/login")));
    }
}
