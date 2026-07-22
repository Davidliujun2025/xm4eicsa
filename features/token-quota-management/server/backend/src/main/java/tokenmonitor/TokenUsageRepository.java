package tokenmonitor;


import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Persistence port. Replace implementation with JDBC without changing services or HTTP API. */
public interface TokenUsageRepository {
    /** @return true when inserted; false when idempotency key already exists. */
    boolean saveIfAbsent(TokenUsageEvent event);

    List<TokenUsageEvent> find(
            String userId,
            Instant fromInclusive,
            Instant toExclusive,
            Provider provider,
            String model,
            int limit);

    List<TokenUsageEvent> findPage(
            String userId,
            Instant fromInclusive,
            Instant toExclusive,
            Provider provider,
            String model,
            int offset,
            int limit);

    long count(
            String userId,
            Instant fromInclusive,
            Instant toExclusive,
            Provider provider,
            String model);

    /** Finds one call detail while enforcing owner isolation in the persistence layer. */
    Optional<TokenUsageEvent> findByRequestId(String userId, String requestId);
}
