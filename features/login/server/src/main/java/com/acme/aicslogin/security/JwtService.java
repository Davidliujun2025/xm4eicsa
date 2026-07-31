package com.acme.aicslogin.security;

import com.acme.aicslogin.config.AuthProperties;
import com.acme.aicslogin.user.CustomerServiceUser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 30;

    private final AuthProperties properties;
    private final byte[] secret;
    private final Clock clock;

    @Autowired
    public JwtService(AuthProperties properties) {
        this(properties, Clock.systemUTC());
    }

    JwtService(AuthProperties properties, Clock clock) {
        this.properties = properties;
        this.secret = Base64.getDecoder().decode(properties.jwtSecretBase64());
        if (secret.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 256 bits");
        }
        this.clock = clock;
    }

    public String createAccessToken(CustomerServiceUser user) {
        Instant issuedAt = clock.instant();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plus(properties.accessTtl())))
                .claim("account", user.getAccount())
                .claim("role", user.getRoleType().name())
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        try {
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to sign access token", exception);
        }
    }

    public Optional<Long> parseValidSubject(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())
                    || !jwt.verify(new MACVerifier(secret))) {
                return Optional.empty();
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Instant now = clock.instant();
            if (!properties.issuer().equals(claims.getIssuer())
                    || claims.getExpirationTime() == null
                    || claims.getExpirationTime().toInstant().plusSeconds(ALLOWED_CLOCK_SKEW_SECONDS).isBefore(now)
                    || claims.getIssueTime() == null
                    || claims.getIssueTime().toInstant().minusSeconds(ALLOWED_CLOCK_SKEW_SECONDS).isAfter(now)) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JOSEException | ParseException | NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
