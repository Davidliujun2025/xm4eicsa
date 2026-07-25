package com.carepilot.tokenquota.dto;

public record TokenQuotaSummaryResponse(
        long totalAccounts,
        long totalDailyQuota,
        long totalUsedTokens,
        double overallUsageRatePercent,
        long highUsageCount,
        long exceededCount
) {
}
