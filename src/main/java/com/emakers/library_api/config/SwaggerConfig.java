package com.emakers.library_api.config;

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
public class SwaggerConfig {

    @Bean
    public OpenAPI customAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                )
                .info(new Info()
                        .title("Library API - Desafio Trainee")
                        .version("1.0.0")
                        .description("API RESTful para gerenciamento de livros e empréstimos.")
                        .contact(new Contact()
                                .name("Seu Nome")
                                .email("seu.email@exemplo.com")
                                .url("https://github.com/seu-usuario")
                        )
                        .license(new License()
                                .name("Licença MIT")
                                .url("https://opensource.org/licenses/MIT")
                        )
                );
    }
}
