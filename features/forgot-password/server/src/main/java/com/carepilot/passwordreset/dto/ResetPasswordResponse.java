package com.carepilot.passwordreset.dto;

public record ResetPasswordResponse(
        boolean reset,
        String nextAction
) {
}
