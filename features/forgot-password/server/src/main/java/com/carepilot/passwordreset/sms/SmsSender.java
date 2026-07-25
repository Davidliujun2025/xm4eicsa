package com.carepilot.passwordreset.sms;

import java.time.Duration;

public interface SmsSender {
    void sendPasswordResetCode(String phone, String code, Duration ttl);
}
