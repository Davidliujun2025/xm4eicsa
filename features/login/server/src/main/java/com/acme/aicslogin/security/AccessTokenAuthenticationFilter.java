package com.acme.aicslogin.security;

import com.acme.aicslogin.user.CustomerServiceUserRepository;
import com.acme.aicslogin.user.UserStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

@Component
public class AccessTokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String ACCESS_COOKIE = "AICS_ACCESS";

    private final JwtService jwtService;
    private final CustomerServiceUserRepository userRepository;

    public AccessTokenAuthenticationFilter(JwtService jwtService, CustomerServiceUserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            findCookie(request, ACCESS_COOKIE)
                    .flatMap(jwtService::parseValidSubject)
                    .flatMap(userRepository::findById)
                    .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                    .ifPresent(user -> {
                        AuthenticatedUser principal = new AuthenticatedUser(
                                user.getId(), user.getAccount(), user.getDisplayName());
                        UsernamePasswordAuthenticationToken authentication =
                                UsernamePasswordAuthenticationToken.authenticated(
                                        principal, null, Collections.emptyList());
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
        }
        filterChain.doFilter(request, response);
    }

    private java.util.Optional<String> findCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return java.util.Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
