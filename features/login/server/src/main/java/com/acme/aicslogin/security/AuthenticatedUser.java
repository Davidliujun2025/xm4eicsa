package com.acme.aicslogin.security;

public record AuthenticatedUser(Long id, String account, String displayName) {
}
