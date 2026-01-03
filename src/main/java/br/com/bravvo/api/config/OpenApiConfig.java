package br.com.bravvo.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Swagger/OpenAPI para habilitar o botão "Authorize"
 * com autenticação Bearer JWT.
 *
 * Resultado:
 * - Swagger mostra o botão "Authorize"
 * - Você cola: Bearer <seu_token>
 * - E consegue testar endpoints protegidos diretamente na UI
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {

        // Define o esquema de segurança "bearerAuth"
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                // registra o esquema no components
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerScheme)
                )
                // aplica o esquema como padrão (para todos os endpoints)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
