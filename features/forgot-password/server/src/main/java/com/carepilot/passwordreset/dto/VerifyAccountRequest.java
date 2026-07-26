package com.carepilot.passwordreset.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyAccountRequest(
        @NotBlank(message = "请输入用户名")
        String username
) {
}
