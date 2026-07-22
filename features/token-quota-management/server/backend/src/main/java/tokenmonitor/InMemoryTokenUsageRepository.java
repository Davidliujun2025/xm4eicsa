package tokenmonitor;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Thread-safe development repository. Idempotency is atomic through putIfAbsent. */
public final class InMemoryTokenUsageRepository implements TokenUsageRepository {
    private final ConcurrentMap<String, TokenUsageEvent> eventsByIdempotencyKey = new ConcurrentHashMap<>();

    @Override
    public boolean saveIfAbsent(TokenUsageEvent event) {
        return eventsByIdempotencyKey.putIfAbsent(event.idempotencyKey(), event) == null;
    }

    @Override
    public List<TokenUsageEvent> find(String userId, Instant fromInclusive, Instant toExclusive,
                                      Provider provider, String model, int limit) {
        int boundedLimit = limit <= 0 ? Integer.MAX_VALUE : Math.max(1, Math.min(limit, 1_000));
        List<TokenUsageEvent> result = matching(userId, fromInclusive, toExclusive, provider, model);
        return List.copyOf(result.subList(0, Math.min(result.size(), boundedLimit)));
    }

    @Override
    public List<TokenUsageEvent> findPage(String userId, Instant fromInclusive, Instant toExclusive,
                                          Provider provider, String model, int offset, int limit) {
        List<TokenUsageEvent> result = matching(userId, fromInclusive, toExclusive, provider, model);
        int from = Math.min(Math.max(0, offset), result.size());
        int to = Math.min(result.size(), from + Math.max(1, limit));
        return List.copyOf(result.subList(from, to));
    }

    @Override
    public long count(String userId, Instant fromInclusive, Instant toExclusive,
                      Provider provider, String model) {
        return matching(userId, fromInclusive, toExclusive, provider, model).size();
    }

    private List<TokenUsageEvent> matching(String userId, Instant fromInclusive, Instant toExclusive,
                                            Provider provider, String model) {
        List<TokenUsageEvent> result = new ArrayList<>();
        for (TokenUsageEvent event : eventsByIdempotencyKey.values()) {
            if (!event.userId().equals(userId)) continue;
            if (event.occurredAt().isBefore(fromInclusive) || !event.occurredAt().isBefore(toExclusive)) continue;
            if (provider != null && event.provider() != provider) continue;
            if (model != null && !model.isBlank() && !event.model().equals(model)) continue;
            result.add(event);
        }
        result.sort(Comparator.comparing(TokenUsageEvent::occurredAt).reversed()
                .thenComparing(TokenUsageEvent::idempotencyKey));
        return result;
    }

    @Override
    public Optional<TokenUsageEvent> findByRequestId(String userId, String requestId) {
        return eventsByIdempotencyKey.values().stream()
                .filter(event -> event.userId().equals(userId) && event.requestId().equals(requestId))
                .max(Comparator.comparing(TokenUsageEvent::occurredAt));
    }
}
