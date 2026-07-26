package com.acme.aicslogin.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuthAuditLogger {

    private static final Logger audit = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void loginResult(String normalizedAccount, String clientIp, String resultCode) {
        audit.info("event=login account={} ipHash={} result={}",
                maskAccount(normalizedAccount), TokenHash.sha256(clientIp == null ? "unknown" : clientIp), resultCode);
    }

    public void logout(Long userId) {
        audit.info("event=logout userId={} result=OK", userId == null ? "unknown" : userId);
    }

    private String maskAccount(String account) {
        if (account == null || account.isBlank()) {
            return "<empty>";
        }
        int visible = Math.min(2, account.length());
        return account.substring(0, visible) + "***";
    }
}
