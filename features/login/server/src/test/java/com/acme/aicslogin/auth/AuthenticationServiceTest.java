package com.acme.aicslogin.auth;

import com.acme.aicslogin.api.BusinessException;
import com.acme.aicslogin.auth.dto.LoginRequest;
import com.acme.aicslogin.security.JwtService;
import com.acme.aicslogin.user.CustomerServiceUser;
import com.acme.aicslogin.user.CustomerServiceUserRepository;
import com.acme.aicslogin.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationServiceTest {

    private final CustomerServiceUserRepository repository = mock(CustomerServiceUserRepository.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final RefreshTokenService refreshTokenService = mock(RefreshTokenService.class);
    private final AuthRateLimiter rateLimiter = mock(AuthRateLimiter.class);
    private final AuthAuditLogger auditLogger = mock(AuthAuditLogger.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationService(
                repository, passwordEncoder, jwtService, refreshTokenService, rateLimiter, auditLogger);
    }

    @Test
    void logsInWithNormalizedAccountAndReturnsRedirect() {
        CustomerServiceUser user = CustomerServiceUser.create(
                "demo.agent", passwordEncoder.encode("AiService2026!"), "演示客服", UserStatus.ACTIVE);
        when(repository.findByAccount("demo.agent")).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(user)).thenReturn("access");
        when(refreshTokenService.issue(null, true)).thenReturn(
                new RefreshTokenService.IssuedRefreshToken("refresh", true, Duration.ofDays(30)));

        AuthSession session = service.login(
                new LoginRequest("  Demo.Agent ", "AiService2026!", true), "127.0.0.1");

        assertThat(session.result().redirectPath()).isEqualTo("/ai-customer-service");
        assertThat(session.result().user().account()).isEqualTo("demo.agent");
        verify(rateLimiter).clearFailures("demo.agent");
        verify(repository).save(user);
    }

    @Test
    void distinguishesUnknownAccountFromWrongPassword() {
        when(repository.findByAccount("missing.agent")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(
                new LoginRequest("missing.agent", "WrongPassword1!", false), "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("ACCOUNT_NOT_FOUND");
                    assertThat(exception.getMessage()).isEqualTo("账号不存在，请找管理员");
                });

        CustomerServiceUser user = CustomerServiceUser.create(
                "demo.agent", passwordEncoder.encode("AiService2026!"), "演示客服", UserStatus.ACTIVE);
        when(repository.findByAccount("demo.agent")).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.login(
                new LoginRequest("demo.agent", "WrongPassword1!", false), "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("INVALID_CREDENTIALS");
                    assertThat(exception.getMessage()).isEqualTo("账号或密码不正确");
                });
        verify(rateLimiter, org.mockito.Mockito.times(2)).recordFailure(any());
    }
}
