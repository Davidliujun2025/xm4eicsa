package com.acme.aicslogin.auth.dto;

public record LoginResult(UserSummary user, String token, String redirectPath) {
}
