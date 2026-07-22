package tokenmonitor;

import java.math.BigDecimal;
import java.time.Instant;

/** Calculated daily utilization, high-usage state and failure anomaly state. */
public record TokenUsageStatus(
        String userId,
        long usedTokens,
        Long dailyTokenLimit,
        BigDecimal usageRate,
        Long remainingTokens,
        Level level,
        boolean highUsage,
        long successfulCalls,
        long failedCalls,
        BigDecimal failureRate,
        boolean abnormal,
        String abnormalReason,
        Instant generatedAt) {

    public enum Level {
        UNCONFIGURED,
        NORMAL,
        HIGH,
        EXHAUSTED
    }
}
