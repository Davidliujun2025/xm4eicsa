package com.carepilot.passwordreset.service;

import com.carepilot.common.BusinessException;
import com.carepilot.common.ErrorCode;
import com.carepilot.passwordreset.config.PasswordResetProperties;
import com.carepilot.passwordreset.domain.AccountRecord;
import com.carepilot.passwordreset.domain.ResetSession;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import com.carepilot.passwordreset.repository.ResetSessionRepository;
import org.springframework.stereotype.Service;

@Service
public class ResetTokenService {
    private final PasswordResetProperties properties;
    private final Clock clock;
    private final ResetSessionRepository resetSessionRepository;

    public ResetTokenService(
            PasswordResetProperties properties,
            Clock clock,
            ResetSessionRepository resetSessionRepository
    ) {
        this.properties = properties;
        this.clock = clock;
        this.resetSessionRepository = resetSessionRepository;
    }

    public ResetSession createSession(AccountRecord account) {
        cleanupExpiredSessions();
        String token = UUID.randomUUID().toString();
        ResetSession session = new ResetSession(
                token,
                account.id(),
                account.username(),
                Instant.now(clock).plus(properties.getResetTokenTtl())
        );
        resetSessionRepository.save(session);
        return session;
    }

    public ResetSession getRequiredSession(String token) {
        cleanupExpiredSessions();
        return resetSessionRepository.findActiveByToken(token, Instant.now(clock))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESET_TOKEN_INVALID,
                        "重置凭证已过期，请重新校验账号"
                ));
    }

    public void consume(String token) {
        resetSessionRepository.consume(token);
    }

    private void cleanupExpiredSessions() {
        resetSessionRepository.cleanupExpired(Instant.now(clock));
    }
}
