package com.acme.aicslogin.auth.dto;

import com.acme.aicslogin.user.CustomerServiceUser;

public record UserSummary(Long id, String account, String displayName) {

    public static UserSummary from(CustomerServiceUser user) {
        return new UserSummary(user.getId(), user.getAccount(), user.getDisplayName());
    }
}
