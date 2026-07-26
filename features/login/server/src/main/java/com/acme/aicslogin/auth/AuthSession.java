package com.acme.aicslogin.auth;

import com.acme.aicslogin.auth.dto.LoginResult;

public record AuthSession(
        LoginResult result,
        String accessToken,
        RefreshTokenService.IssuedRefreshToken refreshToken
) {
}
