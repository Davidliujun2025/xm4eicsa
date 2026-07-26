package com.carepilot.passwordreset.repository;

import com.carepilot.passwordreset.domain.ResetSession;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryResetSessionRepository implements ResetSessionRepository {
    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();

    @Override
    public void save(ResetSession session) {
        sessions.put(session.token(), new Entry(session, null));
    }

    @Override
    public Optional<ResetSession> findActiveByToken(String token, Instant now) {
        Entry entry = sessions.get(token);
        if (entry == null || entry.consumedAt() != null || entry.session().expiresAt().isBefore(now)) {
            return Optional.empty();
        }
        return Optional.of(entry.session());
    }

    @Override
    public void consume(String token) {
        sessions.computeIfPresent(token, (key, old) -> new Entry(old.session(), Instant.now()));
    }

    @Override
    public void cleanupExpired(Instant now) {
        sessions.entrySet().removeIf(entry -> entry.getValue().session().expiresAt().isBefore(now));
    }

    private record Entry(
            ResetSession session,
            Instant consumedAt
    ) {
    }
}
