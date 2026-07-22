package tokenmonitor;

import java.time.Instant;

public record TokenUsageSummary(
        String userId,
        long inputTokens,
        long outputTokens,
        long cachedInputTokens,
        long totalTokens,
        long successfulCalls,
        long failedCalls,
        long averageResponseTimeMs,
        Instant from,
        Instant to,
        Instant generatedAt) {
}
