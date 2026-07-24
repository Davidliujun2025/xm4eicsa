package com.acme.aicslogin.auth;

import com.acme.aicslogin.api.BusinessException;
import com.acme.aicslogin.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    @SuppressWarnings("unchecked")
    private final SetOperations<String, String> sets = mock(SetOperations.class);
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForSet()).thenReturn(sets);
        AuthProperties properties = new AuthProperties(
                "test", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                Duration.ofMinutes(15), Duration.ofHours(8), Duration.ofDays(30), false, List.of());
        service = new RefreshTokenService(redis, properties);
    }

    @Test
    void storesOnlyDigestAndUsesRememberedTtl() {
        RefreshTokenService.IssuedRefreshToken issued = service.issue(42L, true);

        assertThat(issued.value()).hasSize(43);
        assertThat(issued.ttl()).isEqualTo(Duration.ofDays(30));
        String digest = TokenHash.sha256(issued.value());
        verify(values).set("auth:refresh:" + digest, "42:true", Duration.ofDays(30));
        verify(sets).add("auth:user-refresh:42", new String[]{digest});
    }

    @Test
    void consumesTokenOnceAndRejectsReplay() {
        String raw = "one-time-refresh-token";
        String key = "auth:refresh:" + TokenHash.sha256(raw);
        when(values.getAndDelete(key)).thenReturn("42:true").thenReturn(null);

        RefreshTokenService.RefreshSession session = service.consume(raw);
        assertThat(session.userId()).isEqualTo(42L);
        assertThat(session.remembered()).isTrue();
        verify(sets).remove(eq("auth:user-refresh:42"), anyString());

        assertThatThrownBy(() -> service.consume(raw))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("REFRESH_TOKEN_INVALID"));
    }
}
