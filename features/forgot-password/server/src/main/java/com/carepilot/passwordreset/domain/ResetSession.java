package com.carepilot.passwordreset.domain;

import java.time.Instant;

public record ResetSession(
        String token,
        Long accountId,
        String username,
        Instant expiresAt
) {
}
