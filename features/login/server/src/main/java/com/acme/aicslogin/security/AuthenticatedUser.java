package com.acme.aicslogin.security;

import com.acme.aicslogin.user.RoleType;

public record AuthenticatedUser(Long id, String account, String displayName, RoleType roleType) {
}
