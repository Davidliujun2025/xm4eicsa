package com.carepilot.passwordreset.dto;

public record SendSmsCodeResponse(
        long expiresInSeconds,
        long cooldownSeconds
) {
}
