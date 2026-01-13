package br.com.bravvo.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração global de CORS (Cross-Origin Resource Sharing) da aplicação.
 *
 * Esta classe define quais origens externas (frontends) podem
 * acessar a API, quais métodos HTTP são permitidos e
 * se credenciais podem ser enviadas.
 *
 * IMPORTANTE:
 * - Esta configuração atua no nível do Spring MVC
 * - Para funcionar corretamente com Spring Security,
 *   também é necessário habilitar CORS no SecurityConfig
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configura as regras de CORS para todos os endpoints da aplicação.
     *
     * @param registry objeto responsável pelo registro das regras de CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                /**
                 * Define quais origens (domínios) podem acessar a API.
                 *
                 * Neste caso:
                 * - Frontend rodando em http://localhost:3000
                 *
                 * Observação:
                 * - Não é possível usar "*" quando allowCredentials(true) está ativo
                 */
                .allowedOrigins("http://localhost:3000","http://192.168.1.104:3000", "https://frontend-bravvo.netlify.app/", "https://landingpage-bravvo.netlify.app/", "http://100.104.191.40:3000")

                /**
                 * Define quais métodos HTTP são permitidos nas requisições CORS.
                 *
                 * Inclui:
                 * - OPTIONS (pré-flight do navegador)
                 */
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")

                /**
                 * Define quais headers podem ser enviados pelo cliente.
                 *
                 * "*" permite todos, incluindo:
                 * - Authorization
                 * - Content-Type
                 * - Accept
                 */
                .allowedHeaders("*")

                /**
                 * Permite o envio de credenciais na requisição.
                 *
                 * Exemplos:
                 * - Authorization (Bearer Token)
                 * - Cookies (caso fossem usados)
                 *
                 * Obrigatório quando o frontend envia JWT no header Authorization
                 */
                .allowCredentials(true);
    }
}
