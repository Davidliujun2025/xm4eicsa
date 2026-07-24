package com.acme.aicslogin.auth;

import com.acme.aicslogin.config.AuthProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    public static final String ACCESS_COOKIE = "AICS_ACCESS";
    public static final String REFRESH_COOKIE = "AICS_REFRESH";
    private static final String ACCESS_PATH = "/api";
    private static final String REFRESH_PATH = "/api/v1/auth";

    private final AuthProperties properties;

    public AuthCookieService(AuthProperties properties) {
        this.properties = properties;
    }

    public void setSessionCookies(
            HttpServletResponse response,
            String accessToken,
            RefreshTokenService.IssuedRefreshToken refreshToken
    ) {
        ResponseCookie.ResponseCookieBuilder access = baseCookie(ACCESS_COOKIE, accessToken, ACCESS_PATH);
        ResponseCookie.ResponseCookieBuilder refresh = baseCookie(
                REFRESH_COOKIE, refreshToken.value(), REFRESH_PATH);
        if (refreshToken.remembered()) {
            access.maxAge(properties.accessTtl());
            refresh.maxAge(refreshToken.ttl());
        }
        response.addHeader(HttpHeaders.SET_COOKIE, access.build().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refresh.build().toString());
    }

    public void clearSessionCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie(ACCESS_COOKIE, ACCESS_PATH).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie(REFRESH_COOKIE, REFRESH_PATH).toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value, String path) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.secureCookies())
                .sameSite("Lax")
                .path(path);
    }

    private ResponseCookie deleteCookie(String name, String path) {
        return baseCookie(name, "", path).maxAge(Duration.ZERO).build();
    }
}
