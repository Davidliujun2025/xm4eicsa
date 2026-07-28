package com.acme.aicslogin.auth;

import com.acme.aicslogin.api.BusinessException;
import com.acme.aicslogin.config.AuthProperties;
import com.acme.aicslogin.auth.dto.LoginRequest;
import com.acme.aicslogin.auth.dto.LoginResult;
import com.acme.aicslogin.auth.dto.UserSummary;
import com.acme.aicslogin.security.JwtService;
import com.acme.aicslogin.user.CustomerServiceUser;
import com.acme.aicslogin.user.CustomerServiceUserRepository;
import com.acme.aicslogin.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private static final String REDIRECT_PATH = "/ai-customer-service";

    private final CustomerServiceUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthRateLimiter rateLimiter;
    private final AuthAuditLogger auditLogger;
    private final AuthProperties authProperties;
    private final String dummyPasswordHash;

    public AuthenticationService(
            CustomerServiceUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuthRateLimiter rateLimiter,
            AuthAuditLogger auditLogger,
            AuthProperties authProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.rateLimiter = rateLimiter;
        this.auditLogger = auditLogger;
        this.authProperties = authProperties;
        this.dummyPasswordHash = passwordEncoder.encode("Dummy-Password-Only-2026!");
    }

    @Transactional
    public AuthSession login(LoginRequest request, String clientIp) {
        String rawInput = request.account() == null ? "" : request.account();
        String normalizedInput = AccountRules.normalize(rawInput);
        boolean phoneInput = AccountRules.isPhone(normalizedInput);
        String loginKey = normalizedInput;

        log.info("LOGIN_DEBUG inputRaw='{}' normalized='{}' clientIp='{}'", rawInput, normalizedInput, clientIp);
        log.info("LOGIN_DEBUG identifierType={} key='{}'", phoneInput ? "PHONE" : "ACCOUNT", loginKey);

        rateLimiter.assertAllowed(loginKey, clientIp);

        boolean valid = phoneInput
                ? AccountRules.isValidPhone(normalizedInput)
                : AccountRules.isValid(normalizedInput);
        if (!valid) {
            rateLimiter.recordFailure(loginKey);
            auditLogger.loginResult(loginKey, clientIp, "ACCOUNT_NOT_FOUND");
            throw accountNotFound(HttpStatus.BAD_REQUEST);
        }

        Optional<CustomerServiceUser> userOptional = phoneInput
                ? userRepository.findByPhone(loginKey)
                : userRepository.findByAccount(loginKey);
        log.info("LOGIN_DEBUG lookupBy={} resultPresent={}", phoneInput ? "PHONE" : "ACCOUNT", userOptional.isPresent());

        CustomerServiceUser user = userOptional.orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            rateLimiter.recordFailure(loginKey);
            auditLogger.loginResult(loginKey, clientIp, "ACCOUNT_NOT_FOUND");
            throw accountNotFound(HttpStatus.UNAUTHORIZED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            verifyPassword(request.password(), user);
            rateLimiter.recordFailure(loginKey);
            auditLogger.loginResult(loginKey, clientIp, "ACCOUNT_NOT_FOUND");
            throw accountNotFound(HttpStatus.UNAUTHORIZED);
        }
        if (!verifyPassword(request.password(), user)) {
            rateLimiter.recordFailure(loginKey);
            auditLogger.loginResult(loginKey, clientIp, "INVALID_CREDENTIALS");
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "账号或密码不正确"
            );
        }

        rateLimiter.clearFailures(loginKey);
        user.markLoggedIn(Instant.now());
        userRepository.save(user);
        auditLogger.loginResult(loginKey, clientIp, "OK");
        return createSession(user, request.rememberMe());
    }

    private boolean verifyPassword(String rawPassword, CustomerServiceUser user) {
        String passwordHash = user.getPasswordHash();
        if (passwordHash != null && !passwordHash.isBlank()) {
            if (looksLikeBcryptHash(passwordHash)) {
                return passwordEncoder.matches(rawPassword, passwordHash);
            }
            // Temporary local debugging fallback for legacy plaintext records in password_hash.
            if (authProperties.allowPlainPasswordDebug()) {
                return passwordHash.equals(rawPassword);
            }
            return false;
        }

        // Temporary local debugging fallback for legacy plaintext records.
        if (authProperties.allowPlainPasswordDebug()) {
            String legacyPassword = user.getLegacyPassword();
            return legacyPassword != null && legacyPassword.equals(rawPassword);
        }
        return false;
    }

    private boolean looksLikeBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    @Transactional(readOnly = true)
    public AuthSession refresh(String rawRefreshToken) {
        RefreshTokenService.RefreshSession previous = refreshTokenService.consume(rawRefreshToken);
        Long userId = Objects.requireNonNull(previous.userId(), "refresh session userId must not be null");
        CustomerServiceUser user = userRepository.findById(userId)
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> {
                refreshTokenService.revokeAll(userId);
                    return new BusinessException(
                            HttpStatus.UNAUTHORIZED,
                            "REFRESH_TOKEN_INVALID",
                            "登录状态已失效，请重新登录"
                    );
                });
        return createSession(user, previous.remembered());
    }

    public void logout(String rawRefreshToken, Long userId) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
        auditLogger.logout(userId);
    }

    private AuthSession createSession(CustomerServiceUser user, boolean remembered) {
        String accessToken = jwtService.createAccessToken(user);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user.getId(), remembered);
        LoginResult result = new LoginResult(UserSummary.from(user), accessToken, REDIRECT_PATH);
        return new AuthSession(result, accessToken, refreshToken);
    }

    private BusinessException accountNotFound(HttpStatus status) {
        return new BusinessException(
                status,
                "ACCOUNT_NOT_FOUND",
                "账号不存在，请找管理员"
        );
    }
}
