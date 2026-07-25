package com.carepilot.passwordreset.repository;

import com.carepilot.passwordreset.domain.ResetSession;
import java.time.Instant;
import java.util.Optional;

public interface ResetSessionRepository {
    void save(ResetSession session);

    Optional<ResetSession> findActiveByToken(String token, Instant now);

    void consume(String token);

    void cleanupExpired(Instant now);
}
