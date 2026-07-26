package com.carepilot.passwordreset.domain;

import java.time.Instant;

public record VerificationCodeRecord(
        Long id,
        String codeHash,
        Instant expiresAt,
        Instant nextAllowedAt
) {
}
