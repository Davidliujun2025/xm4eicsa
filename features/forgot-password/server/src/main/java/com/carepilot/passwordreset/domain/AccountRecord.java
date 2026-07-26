package com.carepilot.passwordreset.domain;

public record AccountRecord(
        Long id,
        String username,
        String phone,
        String passwordHash,
        boolean enabled
) {
}
