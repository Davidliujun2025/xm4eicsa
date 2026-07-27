package com.acme.aicslogin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
        String issuer,
        String jwtSecretBase64,
        Duration accessTtl,
        Duration sessionRefreshTtl,
        Duration rememberedRefreshTtl,
        boolean secureCookies,
        List<String> allowedOrigins,
        boolean allowPlainPasswordDebug
) {
}
