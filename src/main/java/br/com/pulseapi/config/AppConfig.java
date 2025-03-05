package br.com.pulseapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuração geral da aplicação, incluindo beans necessários.
 */
@Configuration
public class AppConfig {

    /**
     * Define um bean RestTemplate para uso na aplicação.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
