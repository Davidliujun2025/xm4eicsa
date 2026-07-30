package com.acme.aicslogin.auth;

import com.acme.aicslogin.api.ApiResponse;
import com.acme.aicslogin.api.BusinessException;
import com.acme.aicslogin.auth.dto.LoginRequest;
import com.acme.aicslogin.auth.dto.LoginResult;
import com.acme.aicslogin.auth.dto.UserSummary;
import com.acme.aicslogin.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
public class AuthController {

    private final AuthenticationService authenticationService;
    private final AuthCookieService cookieService;

    public AuthController(AuthenticationService authenticationService, AuthCookieService cookieService) {
        this.authenticationService = authenticationService;
        this.cookieService = cookieService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResult> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        AuthSession session = authenticationService.login(request, servletRequest.getRemoteAddr());
        cookieService.setSessionCookies(response, session.accessToken(), session.refreshToken());
        return ApiResponse.ok(session.result());
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResult> refresh(
            @CookieValue(name = AuthCookieService.REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            cookieService.clearSessionCookies(response);
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED,
                    "REFRESH_TOKEN_INVALID",
                    "登录状态已失效，请重新登录"
            );
        }
        AuthSession session = authenticationService.refresh(refreshToken);
        cookieService.setSessionCookies(response, session.accessToken(), session.refreshToken());
        return ApiResponse.ok(session.result());
    }

    @GetMapping("/me")
    public ApiResponse<UserSummary> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.ok(new UserSummary(
                principal.id(),
                principal.account(),
                principal.displayName(),
                com.acme.aicslogin.auth.dto.UserStatusType.ENABLED,
                com.acme.aicslogin.auth.dto.RoleType.CUSTOMER_SERVICE,
                null,
                null,
                principal.id(),
                principal.account(),
                principal.displayName()
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = AuthCookieService.REFRESH_COOKIE, required = false) String refreshToken,
            @AuthenticationPrincipal AuthenticatedUser principal,
            HttpServletResponse response
    ) {
        authenticationService.logout(refreshToken, principal == null ? null : principal.id());
        cookieService.clearSessionCookies(response);
        return ApiResponse.ok(null);
    }
}
