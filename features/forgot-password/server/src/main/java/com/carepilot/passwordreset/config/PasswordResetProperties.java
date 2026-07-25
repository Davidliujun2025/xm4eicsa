package com.carepilot.passwordreset.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "carepilot.password-reset")
public class PasswordResetProperties {
    private Duration resetTokenTtl = Duration.ofMinutes(30);
    private Duration codeTtl = Duration.ofMinutes(5);
    private Duration codeCooldown = Duration.ofSeconds(60);
    private int codeLength = 6;
    private String usernamePattern = "^[A-Za-z0-9_@.-]{4,50}$";

    public Duration getResetTokenTtl() {
        return resetTokenTtl;
    }

    public void setResetTokenTtl(Duration resetTokenTtl) {
        this.resetTokenTtl = resetTokenTtl;
    }

    public Duration getCodeTtl() {
        return codeTtl;
    }

    public void setCodeTtl(Duration codeTtl) {
        this.codeTtl = codeTtl;
    }

    public Duration getCodeCooldown() {
        return codeCooldown;
    }

    public void setCodeCooldown(Duration codeCooldown) {
        this.codeCooldown = codeCooldown;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(int codeLength) {
        this.codeLength = codeLength;
    }

    public String getUsernamePattern() {
        return usernamePattern;
    }

    public void setUsernamePattern(String usernamePattern) {
        this.usernamePattern = usernamePattern;
    }
}
