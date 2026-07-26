package com.carepilot.tokenquota.dto;

public record RecordTokenUsageResponse(
        String accountNo,
        long dailyQuota,
        long usedTokens,
        long remainingTokens,
        long overageTokens,
        double usageRatePercent,
        String statusCode,
        String statusLabel
) {
}
