package br.com.bravvo.api;

import br.com.bravvo.api.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class BravvoApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(BravvoApiApplication.class, args);
    }
}
