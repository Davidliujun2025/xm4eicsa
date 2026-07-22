package tokenmonitor;

import java.time.Instant;

public record TrendPoint(
        Instant bucketStart,
        long inputTokens,
        long outputTokens,
        long totalTokens,
        long calls) {
}
