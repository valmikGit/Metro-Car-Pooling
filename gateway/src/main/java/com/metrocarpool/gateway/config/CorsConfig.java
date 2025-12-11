package com.metrocarpool.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter() {
        logger.info("⚡️ INITIALIZING GLOBAL CORS FILTER ⚡️");
        CorsConfiguration corsConfig = new CorsConfiguration();
        // Use patterns to be more flexible and ensure matching works for localhost/127.0.0.1
        corsConfig.addAllowedOriginPattern("http://localhost:3000");
        corsConfig.addAllowedOriginPattern("http://127.0.0.1:3000");
        corsConfig.addAllowedOriginPattern("http://frontend-service:3000");
        corsConfig.setMaxAge(3600L);
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}