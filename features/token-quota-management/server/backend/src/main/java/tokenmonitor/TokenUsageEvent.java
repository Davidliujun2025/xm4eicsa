package tokenmonitor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable source-of-truth event produced after one model invocation finishes.
 * Token values must come from the provider response, never from local estimation.
 */
public record TokenUsageEvent(
        String idempotencyKey,
        String userId,
        String conversationId,
        String requestId,
        Provider provider,
        String model,
        long inputContentLength,
        long outputContentLength,
        long inputTokens,
        long outputTokens,
        long cachedInputTokens,
        long totalTokens,
        long responseTimeMs,
        CallStatus status,
        Instant occurredAt,
        BigDecimal providerReportedCost,
        String costCurrency) {

    public TokenUsageEvent {
        requireText(idempotencyKey, "idempotencyKey");
        requireText(userId, "userId");
        requireText(requestId, "requestId");
        requireText(model, "model");
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(occurredAt, "occurredAt");
        if (inputContentLength < 0 || outputContentLength < 0) {
            throw new IllegalArgumentException("content lengths must be >= 0");
        }
        if (inputTokens < 0 || outputTokens < 0 || cachedInputTokens < 0 || totalTokens < 0) {
            throw new IllegalArgumentException("token values must be >= 0");
        }
        if (responseTimeMs < 0) throw new IllegalArgumentException("responseTimeMs must be >= 0");
        long minimumTotal = Math.addExact(inputTokens, outputTokens);
        if (totalTokens < minimumTotal) {
            throw new IllegalArgumentException("totalTokens must be >= inputTokens + outputTokens");
        }
        providerReportedCost = providerReportedCost == null ? BigDecimal.ZERO : providerReportedCost;
        costCurrency = costCurrency == null || costCurrency.isBlank() ? "UNKNOWN" : costCurrency.toUpperCase();
    }

    /** Backward-compatible constructor for callers that do not yet report content lengths. */
    public TokenUsageEvent(String idempotencyKey, String userId, String conversationId, String requestId,
                           Provider provider, String model, long inputTokens, long outputTokens,
                           long cachedInputTokens, long totalTokens, long responseTimeMs, CallStatus status,
                           Instant occurredAt, BigDecimal providerReportedCost, String costCurrency) {
        this(idempotencyKey, userId, conversationId, requestId, provider, model, 0, 0,
                inputTokens, outputTokens, cachedInputTokens, totalTokens, responseTimeMs, status,
                occurredAt, providerReportedCost, costCurrency);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }
}
