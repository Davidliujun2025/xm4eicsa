package tokenmonitor;


import java.util.LinkedHashMap;
import java.util.Map;

final class ApiJson {
    private ApiJson() {}

    static Map<String, Object> summary(TokenUsageSummary value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", value.userId());
        map.put("inputTokens", value.inputTokens());
        map.put("outputTokens", value.outputTokens());
        map.put("cachedInputTokens", value.cachedInputTokens());
        map.put("totalTokens", value.totalTokens());
        map.put("successfulCalls", value.successfulCalls());
        map.put("failedCalls", value.failedCalls());
        map.put("averageResponseTimeMs", value.averageResponseTimeMs());
        map.put("empty", value.successfulCalls() + value.failedCalls() == 0);
        map.put("from", value.from().toString());
        map.put("to", value.to().toString());
        map.put("generatedAt", value.generatedAt().toString());
        return map;
    }

    static Map<String, Object> event(TokenUsageEvent value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("idempotencyKey", value.idempotencyKey());
        map.put("conversationId", value.conversationId());
        map.put("requestId", value.requestId());
        map.put("provider", value.provider().name());
        map.put("model", value.model());
        map.put("inputContentLength", value.inputContentLength());
        map.put("outputContentLength", value.outputContentLength());
        map.put("inputTokens", value.inputTokens());
        map.put("outputTokens", value.outputTokens());
        map.put("cachedInputTokens", value.cachedInputTokens());
        map.put("totalTokens", value.totalTokens());
        map.put("responseTimeMs", value.responseTimeMs());
        map.put("status", value.status().name());
        map.put("occurredAt", value.occurredAt().toString());
        map.put("providerReportedCost", value.providerReportedCost());
        map.put("costCurrency", value.costCurrency());
        return map;
    }

    static Map<String, Object> today(TokenUsageSummary summary, TokenUsageStatus status) {
        Map<String, Object> map = new LinkedHashMap<>(summary(summary));
        map.put("dailyTokenLimit", status.dailyTokenLimit());
        map.put("remainingTokens", status.remainingTokens());
        map.put("usageRate", status.usageRate());
        map.put("usagePercent", status.usageRate() == null ? null : status.usageRate().movePointRight(2));
        map.put("level", status.level().name());
        map.put("highUsage", status.highUsage());
        return map;
    }

    static Map<String, Object> overview(TokenUsageOverview value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", value.userId());
        map.put("today", summary(value.today()));
        map.put("thisWeek", summary(value.thisWeek()));
        map.put("thisMonth", summary(value.thisMonth()));
        map.put("history", summary(value.history()));
        map.put("empty", value.empty());
        map.put("generatedAt", value.generatedAt().toString());
        return map;
    }

    static Map<String, Object> page(TokenUsagePage value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("items", value.items().stream().map(ApiJson::event).toList());
        map.put("page", value.page());
        map.put("size", value.size());
        map.put("totalElements", value.totalElements());
        map.put("totalPages", value.totalPages());
        map.put("empty", value.empty());
        return map;
    }

    static Map<String, Object> status(TokenUsageStatus value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", value.userId());
        map.put("usedTokens", value.usedTokens());
        map.put("dailyTokenLimit", value.dailyTokenLimit());
        map.put("usageRate", value.usageRate());
        map.put("remainingTokens", value.remainingTokens());
        map.put("level", value.level().name());
        map.put("highUsage", value.highUsage());
        map.put("successfulCalls", value.successfulCalls());
        map.put("failedCalls", value.failedCalls());
        map.put("failureRate", value.failureRate());
        map.put("abnormal", value.abnormal());
        map.put("abnormalReason", value.abnormalReason());
        map.put("generatedAt", value.generatedAt().toString());
        return map;
    }

    static Map<String, Object> trend(TrendPoint value) {
        return Map.of(
                "bucketStart", value.bucketStart().toString(),
                "inputTokens", value.inputTokens(),
                "outputTokens", value.outputTokens(),
                "totalTokens", value.totalTokens(),
                "calls", value.calls());
    }

    static Map<String, Object> trendSeries(TrendSeries value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", value.userId());
        map.put("from", value.from().toString());
        map.put("to", value.to().toString());
        map.put("items", value.items().stream().map(ApiJson::trend).toList());
        map.put("empty", value.empty());
        return map;
    }

    static Map<String, Object> capability(ProviderCapability value) {
        return Map.of(
                "provider", value.provider().name(),
                "accountBalance", value.accountBalance().name(),
                "tokenPlanRemaining", value.tokenPlanRemaining().name(),
                "historicalUsage", value.historicalUsage().name(),
                "note", value.note(),
                "documentationUrl", value.documentationUrl());
    }

    static Map<String, Object> quota(ProviderQuotaSnapshot value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("provider", value.provider().name());
        map.put("quotaType", value.quotaType());
        map.put("availableBalance", value.availableBalance());
        map.put("currency", value.currency());
        map.put("details", value.details());
        map.put("fetchedAt", value.fetchedAt().toString());
        return map;
    }
}
