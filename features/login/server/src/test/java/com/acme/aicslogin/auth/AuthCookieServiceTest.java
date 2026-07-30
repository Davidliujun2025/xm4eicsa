package com.acme.aicslogin.auth;

import com.acme.aicslogin.config.AuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieServiceTest {

    private final AuthProperties properties = new AuthProperties(
            "test", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
            Duration.ofMinutes(15), Duration.ofHours(8), Duration.ofDays(30), true, List.of(), false);
    private final AuthCookieService service = new AuthCookieService(properties);

    @Test
    void rememberedSessionUsesPersistentSecureHttpOnlyCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.setSessionCookies(response, "access", new RefreshTokenService.IssuedRefreshToken(
                "refresh", true, Duration.ofDays(30)));

        assertThat(response.getHeaders("Set-Cookie"))
                .anySatisfy(header -> assertThat(header)
                        .contains("AICS_ACCESS=access", "Max-Age=900", "Secure", "HttpOnly", "SameSite=Lax"))
                .anySatisfy(header -> assertThat(header)
                        .contains("AICS_REFRESH=refresh", "Max-Age=2592000", "Path=/api/v1/auth"));
    }

    @Test
    void normalSessionDoesNotPersistAcrossBrowserRestart() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.setSessionCookies(response, "access", new RefreshTokenService.IssuedRefreshToken(
                "refresh", false, Duration.ofHours(8)));

        assertThat(response.getHeaders("Set-Cookie")).allSatisfy(header ->
                assertThat(header).doesNotContain("Max-Age"));
    }
}
