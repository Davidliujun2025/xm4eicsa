package com.acme.aicslogin.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ProductionSecurityValidator {

    private static final String DEVELOPMENT_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final AuthProperties properties;

    public ProductionSecurityValidator(AuthProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validate() {
        if (DEVELOPMENT_SECRET.equals(properties.jwtSecretBase64())) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must be replaced in production");
        }
        if (!properties.secureCookies()) {
            throw new IllegalStateException("SECURE_COOKIES must be true in production");
        }
        if (properties.allowedOrigins().stream().anyMatch(origin -> origin.contains("localhost"))) {
            throw new IllegalStateException("ALLOWED_ORIGINS must not contain localhost in production");
        }
    }
}
