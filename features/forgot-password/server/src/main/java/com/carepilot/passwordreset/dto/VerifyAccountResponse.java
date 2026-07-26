package com.carepilot.passwordreset.dto;

public record VerifyAccountResponse(
        String resetToken,
        String username,
        String maskedBoundPhone,
        long expiresInSeconds
) {
}
