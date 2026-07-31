package com.acme.aicslogin.auth;

import com.acme.aicslogin.api.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimiter.class);

    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(30);
    private static final Duration IP_WINDOW = Duration.ofMinutes(5);
    private static final long IP_LIMIT = 60;

    private final StringRedisTemplate redis;

    public AuthRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void assertAllowed(String normalizedAccount, String clientIp) {
        try {
            String accountKey = accountKey(normalizedAccount);
            checkIpLimit(clientIp);

            String rawCount = redis.opsForValue().get(accountKey);
            long failures = rawCount == null ? 0 : Long.parseLong(rawCount);
            if (failures >= 10) {
                acquireGate(accountKey + ":gate:severe", Duration.ofMinutes(5));
            } else if (failures >= 5) {
                acquireGate(accountKey + ":gate:moderate", Duration.ofSeconds(30));
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable for rate limiting, skipping throttle checks", ex);
        }
    }

    public void recordFailure(String normalizedAccount) {
        try {
            String key = accountKey(normalizedAccount);
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, FAILURE_WINDOW);
            }
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable while recording login failure", ex);
        }
    }

    public void clearFailures(String normalizedAccount) {
        try {
            String key = accountKey(normalizedAccount);
            redis.delete(key);
            redis.delete(key + ":gate:moderate");
            redis.delete(key + ":gate:severe");
        } catch (RuntimeException ex) {
            log.warn("Redis unavailable while clearing login failures", ex);
        }
    }

    private void checkIpLimit(String clientIp) {
        String key = "auth:login:ip:" + TokenHash.sha256(clientIp == null ? "unknown" : clientIp);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, IP_WINDOW);
        }
        if (count != null && count > IP_LIMIT) {
            throw tooManyAttempts();
        }
    }

    private void acquireGate(String key, Duration duration) {
        Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", duration);
        if (!Boolean.TRUE.equals(acquired)) {
            throw tooManyAttempts();
        }
    }

    private String accountKey(String normalizedAccount) {
        return "auth:login:account:" + TokenHash.sha256(normalizedAccount);
    }

    private BusinessException tooManyAttempts() {
        return new BusinessException(
                HttpStatus.TOO_MANY_REQUESTS,
                "TOO_MANY_ATTEMPTS",
                "登录尝试过于频繁，请稍后重试"
        );
    }
}
