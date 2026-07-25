package com.carepilot.agentaccount.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET =
            "create-agent-account-secret-key-2026-create-agent";

    private static final long EXPIRE_TIME = 24 * 60 * 60 * 1000;

public String generateToken(String username) {
    SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    return Jwts.builder()
            .subject(username)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
            .signWith(key)
            .compact();
}
}