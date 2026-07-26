package com.carepilot.passwordreset.sms;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleSmsSender implements SmsSender {
    private static final Logger log = LoggerFactory.getLogger(ConsoleSmsSender.class);

    @Override
    public void sendPasswordResetCode(String phone, String code, Duration ttl) {
        log.info("Password reset SMS code sent. phone={}, code={}, ttlSeconds={}",
                phone, code, ttl.toSeconds());
    }
}
