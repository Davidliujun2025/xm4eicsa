package com.carepilot.tokenquota.service;

import com.carepilot.tokenquota.config.TokenQuotaProperties;
import com.carepilot.tokenquota.domain.TokenQuotaAccount;
import com.carepilot.tokenquota.domain.TokenUsageStatus;
import com.carepilot.tokenquota.dto.RecordTokenUsageResponse;
import com.carepilot.tokenquota.dto.TokenQuotaItemResponse;
import org.springframework.stereotype.Component;

@Component
public class TokenQuotaCalculator {
    private final TokenQuotaProperties properties;

    public TokenQuotaCalculator(TokenQuotaProperties properties) {
        this.properties = properties;
    }

    public TokenUsageStatus calculateStatus(long dailyQuota, long usedTokens) {
        if (usedTokens >= dailyQuota) {
            return TokenUsageStatus.EXCEEDED_RECOMMENDED;
        }
        if (usedTokens >= Math.ceil(dailyQuota * properties.getHighUsageThreshold())) {
            return TokenUsageStatus.HIGH_USAGE;
        }
        return TokenUsageStatus.NORMAL;
    }

    public TokenQuotaItemResponse toItemResponse(TokenQuotaAccount account) {
        TokenUsageStatus status = calculateStatus(account.dailyQuota(), account.usedTokensToday());
        return new TokenQuotaItemResponse(
                account.accountNo(),
                account.accountName(),
                account.dailyQuota(),
                account.usedTokensToday(),
                remainingTokens(account.dailyQuota(), account.usedTokensToday()),
                overageTokens(account.dailyQuota(), account.usedTokensToday()),
                usageRatePercent(account.dailyQuota(), account.usedTokensToday()),
                account.aiCallCountToday(),
                account.businessCountToday(),
                status.getCode(),
                status.getLabel(),
                account.enabled()
        );
    }

    public RecordTokenUsageResponse toUsageResponse(TokenQuotaAccount account) {
        TokenUsageStatus status = calculateStatus(account.dailyQuota(), account.usedTokensToday());
        return new RecordTokenUsageResponse(
                account.accountNo(),
                account.dailyQuota(),
                account.usedTokensToday(),
                remainingTokens(account.dailyQuota(), account.usedTokensToday()),
                overageTokens(account.dailyQuota(), account.usedTokensToday()),
                usageRatePercent(account.dailyQuota(), account.usedTokensToday()),
                status.getCode(),
                status.getLabel()
        );
    }

    private long remainingTokens(long dailyQuota, long usedTokens) {
        return Math.max(0, dailyQuota - usedTokens);
    }

    private long overageTokens(long dailyQuota, long usedTokens) {
        return Math.max(0, usedTokens - dailyQuota);
    }

    private double usageRatePercent(long dailyQuota, long usedTokens) {
        if (dailyQuota <= 0) {
            return 0D;
        }
        return Math.round((usedTokens * 10000D / dailyQuota)) / 100D;
    }
}
