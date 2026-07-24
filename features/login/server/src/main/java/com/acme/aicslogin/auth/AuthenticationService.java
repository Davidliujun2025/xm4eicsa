package com.acme.aicslogin.auth;

import com.acme.aicslogin.api.BusinessException;
import com.acme.aicslogin.auth.dto.LoginRequest;
import com.acme.aicslogin.auth.dto.LoginResult;
import com.acme.aicslogin.auth.dto.UserSummary;
import com.acme.aicslogin.security.JwtService;
import com.acme.aicslogin.user.CustomerServiceUser;
import com.acme.aicslogin.user.CustomerServiceUserRepository;
import com.acme.aicslogin.user.UserStatus;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthenticationService {

    private static final String REDIRECT_PATH = "/ai-customer-service";

    private final CustomerServiceUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthRateLimiter rateLimiter;
    private final AuthAuditLogger auditLogger;
    private final String dummyPasswordHash;

    public AuthenticationService(
            CustomerServiceUserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuthRateLimiter rateLimiter,
            AuthAuditLogger auditLogger
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.rateLimiter = rateLimiter;
        this.auditLogger = auditLogger;
        this.dummyPasswordHash = passwordEncoder.encode("Dummy-Password-Only-2026!");
    }

    @Transactional
    public AuthSession login(LoginRequest request, String clientIp) {
        String account = AccountRules.normalize(request.account());
        rateLimiter.assertAllowed(account, clientIp);

        if (!AccountRules.isValid(account)) {
            rateLimiter.recordFailure(account);
            auditLogger.loginResult(account, clientIp, "ACCOUNT_NOT_FOUND");
            throw accountNotFound(HttpStatus.BAD_REQUEST);
        }

        CustomerServiceUser user = userRepository.findByAccount(account).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            rateLimiter.recordFailure(account);
            auditLogger.loginResult(account, clientIp, "ACCOUNT_NOT_FOUND");
            throw accountNotFound(HttpStatus.UNAUTHORIZED);
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            passwordEncoder.matches(request.password(), user.getPasswordHash());
            rateLimiter.recordFailure(account);
            auditLogger.loginResult(account, clientIp, "ACCOUNT_NOT_FOUND");
            throw accountNotFound(HttpStatus.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimiter.recordFailure(account);
            auditLogger.loginResult(account, clientIp, "INVALID_CREDENTIALS");
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CREDENTIALS",
                    "账号或密码不正确"
            );
        }

        rateLimiter.clearFailures(account);
        user.markLoggedIn(Instant.now());
        userRepository.save(user);
        auditLogger.loginResult(account, clientIp, "OK");
        return createSession(user, request.rememberMe());
    }

    @Transactional(readOnly = true)
    public AuthSession refresh(String rawRefreshToken) {
        RefreshTokenService.RefreshSession previous = refreshTokenService.consume(rawRefreshToken);
        CustomerServiceUser user = userRepository.findById(previous.userId())
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> {
                    refreshTokenService.revokeAll(previous.userId());
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
        LoginResult result = new LoginResult(UserSummary.from(user), REDIRECT_PATH);
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
