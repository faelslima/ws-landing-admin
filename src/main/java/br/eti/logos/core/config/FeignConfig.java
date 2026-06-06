package br.eti.logos.core.config;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    Logger.Level feignLoggerLevel() {
        // BASIC: loga método, URL e status. FULL logaria headers (Authorization: Bearer token) — nunca usar.
        return Logger.Level.BASIC;
    }
}
