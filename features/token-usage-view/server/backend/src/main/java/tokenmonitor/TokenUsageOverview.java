package tokenmonitor;

import java.time.Instant;

/** Fixed cards required by the personal Token page. */
public record TokenUsageOverview(
        String userId,
        TokenUsageSummary today,
        TokenUsageSummary thisWeek,
        TokenUsageSummary thisMonth,
        TokenUsageSummary history,
        boolean empty,
        Instant generatedAt) {
}
