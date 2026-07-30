package com.acme.aicslogin.auth.dto;

import com.acme.aicslogin.user.CustomerServiceUser;
import com.acme.aicslogin.user.UserStatus;

import java.time.Instant;

public record UserSummary(
    Long userId,
    String username,
    String nickName,
    UserStatusType status,
    RoleType roleType,
    Instant createdAt,
    Instant updatedAt,
    Long id,
    String account,
    String displayName
) {

    public static UserSummary from(CustomerServiceUser user) {
    UserStatusType normalizedStatus = user.getStatus() == UserStatus.DISABLED
        ? UserStatusType.DISABLED
        : UserStatusType.ENABLED;
    return new UserSummary(
        user.getId(),
        user.getAccount(),
        user.getDisplayName(),
        normalizedStatus,
        RoleType.CUSTOMER_SERVICE,
        user.getCreatedAt(),
        user.getUpdatedAt(),
        user.getId(),
        user.getAccount(),
        user.getDisplayName()
    );
    }
}
