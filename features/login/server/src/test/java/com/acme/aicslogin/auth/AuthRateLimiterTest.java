package com.acme.aicslogin.auth;

import com.acme.aicslogin.api.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthRateLimiterTest {

    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private AuthRateLimiter limiter;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
        limiter = new AuthRateLimiter(redis);
    }

    @Test
    void opensThirtySecondGateAfterFiveFailures() {
        String accountKey = "auth:login:account:" + TokenHash.sha256("demo.agent");
        String ipKey = "auth:login:ip:" + TokenHash.sha256("127.0.0.1");
        when(values.increment(ipKey)).thenReturn(1L, 2L);
        when(values.get(accountKey)).thenReturn("5");
        when(values.setIfAbsent(accountKey + ":gate:moderate", "1", Duration.ofSeconds(30)))
                .thenReturn(true, false);

        assertThatCode(() -> limiter.assertAllowed("demo.agent", "127.0.0.1"))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.assertAllowed("demo.agent", "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TOO_MANY_ATTEMPTS"));
    }

    @Test
    void rejectsIpAfterSixtyRequestsInFiveMinutes() {
        String ipKey = "auth:login:ip:" + TokenHash.sha256("127.0.0.1");
        when(values.increment(ipKey)).thenReturn(61L);

        assertThatThrownBy(() -> limiter.assertAllowed("demo.agent", "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("TOO_MANY_ATTEMPTS"));
    }
}
