package com.acme.aicslogin.security;

import com.acme.aicslogin.config.AuthProperties;
import com.acme.aicslogin.user.CustomerServiceUser;
import com.acme.aicslogin.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @Test
    void signsValidTokenAndRejectsItAfterExpiryAndSkew() {
        Instant issuedAt = Instant.parse("2026-07-20T00:00:00Z");
        AuthProperties properties = new AuthProperties(
                "test-issuer", SECRET, Duration.ofMinutes(15), Duration.ofHours(8), Duration.ofDays(30), false,
                List.of(), false);
        CustomerServiceUser user = CustomerServiceUser.create(
                "demo.agent", "unused", "演示客服", UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "id", 42L);

        JwtService issuer = new JwtService(properties, Clock.fixed(issuedAt, ZoneOffset.UTC));
        String token = issuer.createAccessToken(user);
        assertThat(issuer.parseValidSubject(token)).contains(42L);

        JwtService expiredVerifier = new JwtService(
                properties, Clock.fixed(issuedAt.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));
        assertThat(expiredVerifier.parseValidSubject(token)).isEmpty();
    }
}
