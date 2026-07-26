package com.acme.aicslogin.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String account,
        @NotBlank String password,
        boolean rememberMe
) {
}
