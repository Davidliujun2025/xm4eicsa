package com.acme.aicslogin.auth;

import com.acme.aicslogin.api.BusinessException;
import com.acme.aicslogin.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private static final String TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String USER_KEY_PREFIX = "auth:user-refresh:";

    private final StringRedisTemplate redis;
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, String> localTokenStore = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> localUserTokenStore = new ConcurrentHashMap<>();

    public RefreshTokenService(StringRedisTemplate redis, AuthProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public IssuedRefreshToken issue(Long userId, boolean remembered) {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        String digest = TokenHash.sha256(rawToken);
        Duration ttl = remembered ? properties.rememberedRefreshTtl() : properties.sessionRefreshTtl();

        try {
            redis.opsForValue().set(TOKEN_KEY_PREFIX + digest, userId + ":" + remembered, ttl);
            String userKey = USER_KEY_PREFIX + userId;
            redis.opsForSet().add(userKey, digest);
            redis.expire(userKey, properties.rememberedRefreshTtl().plusHours(1));
        } catch (RuntimeException ex) {
            // Local fallback keeps development login/refresh usable without Redis.
            localTokenStore.put(digest, userId + ":" + remembered);
            localUserTokenStore.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(digest);
            log.warn("Redis unavailable, fallback to in-memory refresh token store");
        }
        return new IssuedRefreshToken(rawToken, remembered, ttl);
    }

    public RefreshSession consume(String rawToken) {
        String digest = TokenHash.sha256(rawToken);
        String value;
        try {
            value = redis.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + digest);
        } catch (RuntimeException ex) {
            value = localTokenStore.remove(digest);
            log.warn("Redis unavailable, consuming refresh token from in-memory store");
        }
        if (value == null) {
            throw invalidRefreshToken();
        }

        String[] parts = value.split(":", 2);
        if (parts.length != 2) {
            throw invalidRefreshToken();
        }
        try {
            Long userId = Long.parseLong(parts[0]);
            boolean remembered = Boolean.parseBoolean(parts[1]);
            try {
                redis.opsForSet().remove(USER_KEY_PREFIX + userId, digest);
            } catch (RuntimeException ex) {
                Set<String> digests = localUserTokenStore.getOrDefault(userId, Set.of());
                digests.remove(digest);
            }
            return new RefreshSession(userId, remembered);
        } catch (NumberFormatException exception) {
            throw invalidRefreshToken();
        }
    }

    public void revoke(String rawToken) {
        String digest = TokenHash.sha256(rawToken);
        String value;
        try {
            value = redis.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + digest);
        } catch (RuntimeException ex) {
            value = localTokenStore.remove(digest);
        }
        if (value != null) {
            String[] parts = value.split(":", 2);
            if (parts.length > 0) {
                try {
                    redis.opsForSet().remove(USER_KEY_PREFIX + parts[0], digest);
                } catch (RuntimeException ex) {
                    try {
                        Long userId = Long.parseLong(parts[0]);
                        localUserTokenStore.getOrDefault(userId, new HashSet<>()).remove(digest);
                    } catch (NumberFormatException ignore) {
                        // ignore invalid fallback key format
                    }
                }
            }
        }
    }

    public void revokeAll(Long userId) {
        String userKey = USER_KEY_PREFIX + userId;
        try {
            Set<String> digests = redis.opsForSet().members(userKey);
            if (digests != null && !digests.isEmpty()) {
                redis.delete(digests.stream().map(digest -> TOKEN_KEY_PREFIX + digest).toList());
            }
            redis.delete(userKey);
        } catch (RuntimeException ex) {
            Set<String> digests = localUserTokenStore.remove(userId);
            if (digests != null) {
                digests.forEach(localTokenStore::remove);
            }
        }
    }

    private BusinessException invalidRefreshToken() {
        return new BusinessException(
                HttpStatus.UNAUTHORIZED,
                "REFRESH_TOKEN_INVALID",
                "登录状态已失效，请重新登录"
        );
    }

    public record IssuedRefreshToken(String value, boolean remembered, Duration ttl) {
    }

    public record RefreshSession(Long userId, boolean remembered) {
    }
}
