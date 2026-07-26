package com.carepilot.tokenquota.dto;

public record TokenQuotaItemResponse(
        String accountNo,
        String accountName,
        long dailyQuota,
        long usedTokens,
        long remainingTokens,
        long overageTokens,
        double usageRatePercent,
        long aiCallCount,
        long businessCount,
        String statusCode,
        String statusLabel,
        boolean enabled
) {
}
