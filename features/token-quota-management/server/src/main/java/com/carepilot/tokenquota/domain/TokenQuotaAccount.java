package com.carepilot.tokenquota.domain;

import java.time.LocalDate;

public record TokenQuotaAccount(
        Long id,
        String accountNo,
        String accountName,
        long dailyQuota,
        long usedTokensToday,
        long aiCallCountToday,
        long businessCountToday,
        LocalDate usageDate,
        boolean enabled
) {
    public TokenQuotaAccount withDailyQuota(long newDailyQuota) {
        return new TokenQuotaAccount(
                id,
                accountNo,
                accountName,
                newDailyQuota,
                usedTokensToday,
                aiCallCountToday,
                businessCountToday,
                usageDate,
                enabled
        );
    }

    public TokenQuotaAccount withUsage(long usedTokens, long aiCallCount, long businessCount, LocalDate date) {
        return new TokenQuotaAccount(
                id,
                accountNo,
                accountName,
                dailyQuota,
                usedTokens,
                aiCallCount,
                businessCount,
                date,
                enabled
        );
    }
}
