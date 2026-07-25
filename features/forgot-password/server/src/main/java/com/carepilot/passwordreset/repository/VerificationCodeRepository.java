package com.carepilot.passwordreset.repository;

import com.carepilot.passwordreset.domain.VerificationCodeRecord;
import java.time.Instant;
import java.util.Optional;

public interface VerificationCodeRepository {
    Optional<VerificationCodeRecord> findLatestUnconsumed(String resetToken, String phone);

    void save(String resetToken, Long accountId, String account, String phone, String codeHash,
              Instant expiresAt, Instant nextAllowedAt);

    void consume(Long id);

    void cleanupExpired(Instant now);
}
