package com.acme.aicslogin.auth;

import com.acme.aicslogin.api.BusinessException;
import com.acme.aicslogin.config.AuthProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

@Service
public class RefreshTokenService {

    private static final String TOKEN_KEY_PREFIX = "auth:refresh:";
    private static final String USER_KEY_PREFIX = "auth:user-refresh:";

    private final StringRedisTemplate redis;
    private final AuthProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

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

        redis.opsForValue().set(TOKEN_KEY_PREFIX + digest, userId + ":" + remembered, ttl);
        String userKey = USER_KEY_PREFIX + userId;
        redis.opsForSet().add(userKey, digest);
        redis.expire(userKey, properties.rememberedRefreshTtl().plusHours(1));
        return new IssuedRefreshToken(rawToken, remembered, ttl);
    }

    public RefreshSession consume(String rawToken) {
        String digest = TokenHash.sha256(rawToken);
        String value = redis.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + digest);
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
            redis.opsForSet().remove(USER_KEY_PREFIX + userId, digest);
            return new RefreshSession(userId, remembered);
        } catch (NumberFormatException exception) {
            throw invalidRefreshToken();
        }
    }

    public void revoke(String rawToken) {
        String digest = TokenHash.sha256(rawToken);
        String value = redis.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + digest);
        if (value != null) {
            String[] parts = value.split(":", 2);
            if (parts.length > 0) {
                redis.opsForSet().remove(USER_KEY_PREFIX + parts[0], digest);
            }
        }
    }

    public void revokeAll(Long userId) {
        String userKey = USER_KEY_PREFIX + userId;
        Set<String> digests = redis.opsForSet().members(userKey);
        if (digests != null && !digests.isEmpty()) {
            redis.delete(digests.stream().map(digest -> TOKEN_KEY_PREFIX + digest).toList());
        }
        redis.delete(userKey);
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
