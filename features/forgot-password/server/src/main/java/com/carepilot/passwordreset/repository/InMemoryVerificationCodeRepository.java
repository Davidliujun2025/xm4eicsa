package com.carepilot.passwordreset.repository;

import com.carepilot.passwordreset.domain.VerificationCodeRecord;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryVerificationCodeRepository implements VerificationCodeRepository {
    private final AtomicLong idSequence = new AtomicLong(1);
    private final Map<Long, Entry> codes = new ConcurrentHashMap<>();

    @Override
    public Optional<VerificationCodeRecord> findLatestUnconsumed(String resetToken, String phone) {
        return codes.values()
                .stream()
                .filter(entry -> entry.resetToken().equals(resetToken))
                .filter(entry -> entry.phone().equals(phone))
                .filter(entry -> entry.consumedAt() == null)
                .max(Comparator.comparing(Entry::id))
                .map(entry -> new VerificationCodeRecord(
                        entry.id(),
                        entry.codeHash(),
                        entry.expiresAt(),
                        entry.nextAllowedAt()
                ));
    }

    @Override
    public void save(String resetToken, Long accountId, String account, String phone, String codeHash,
                     Instant expiresAt, Instant nextAllowedAt) {
        long id = idSequence.getAndIncrement();
        codes.put(id, new Entry(id, resetToken, phone, codeHash, expiresAt, nextAllowedAt, null));
    }

    @Override
    public void consume(Long id) {
        codes.computeIfPresent(id, (key, old) -> new Entry(
                old.id(),
                old.resetToken(),
                old.phone(),
                old.codeHash(),
                old.expiresAt(),
                old.nextAllowedAt(),
                Instant.now()
        ));
    }

    @Override
    public void cleanupExpired(Instant now) {
        codes.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private record Entry(
            Long id,
            String resetToken,
            String phone,
            String codeHash,
            Instant expiresAt,
            Instant nextAllowedAt,
            Instant consumedAt
    ) {
    }
}
