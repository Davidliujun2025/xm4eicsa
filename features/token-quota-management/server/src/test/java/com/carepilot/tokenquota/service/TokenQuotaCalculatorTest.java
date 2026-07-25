package com.carepilot.tokenquota.service;

import com.carepilot.tokenquota.config.TokenQuotaProperties;
import com.carepilot.tokenquota.domain.TokenUsageStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenQuotaCalculatorTest {

    private final TokenQuotaCalculator calculator = new TokenQuotaCalculator(new TokenQuotaProperties());

    @Test
    void returnsNormalWhenUsageBelowThreshold() {
        assertEquals(TokenUsageStatus.NORMAL, calculator.calculateStatus(100_000, 79_999));
    }

    @Test
    void returnsHighUsageWhenUsageReachesEightyPercent() {
        assertEquals(TokenUsageStatus.HIGH_USAGE, calculator.calculateStatus(100_000, 80_000));
    }

    @Test
    void returnsExceededWhenUsageReachesDailyQuota() {
        assertEquals(TokenUsageStatus.EXCEEDED_RECOMMENDED, calculator.calculateStatus(100_000, 100_000));
    }
}
