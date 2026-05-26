package com.emakers.library_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Library API - Desafio Trainee")
                        .version("1.0.0")
                        .description("API RESTful para gerenciamento de livros e empréstimos desenvolvida" +
                                " para o processo seletivo da Emakers.")
                        .contact(new Contact()
                                .name("Marco Túlio")
                                .email("marco.silva5@estudante.ufla.br")
                                .url("https://github.com/MarcoTFranco")
                        )
                        .license(new License()
                                .name("Licença MIT")
                                .url("https://opensource.org/licenses/MIT")
                        )
                );
    }
}
