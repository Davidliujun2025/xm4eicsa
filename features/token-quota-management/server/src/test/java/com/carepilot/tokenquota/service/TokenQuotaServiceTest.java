package com.carepilot.tokenquota.service;

import com.carepilot.tokenquota.config.TokenQuotaProperties;
import com.carepilot.tokenquota.dto.BatchUpdateDailyQuotaRequest;
import com.carepilot.tokenquota.dto.RecordTokenUsageRequest;
import com.carepilot.tokenquota.dto.RecordTokenUsageResponse;
import com.carepilot.tokenquota.dto.TokenQuotaItemResponse;
import com.carepilot.tokenquota.dto.UpdateDailyQuotaRequest;
import com.carepilot.tokenquota.repository.InMemoryTokenQuotaAdjustmentLogRepository;
import com.carepilot.tokenquota.repository.InMemoryTokenQuotaRepository;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TokenQuotaServiceTest {

    @Test
    void updatesQuotaAndRecordsAdjustmentLog() {
        TokenQuotaService service = buildService();

        service.updateDailyQuota("CS1001", new UpdateDailyQuotaRequest(
                150_000L,
                "根据业务量上调",
                "admin-1",
                "管理员"
        ));

        TokenQuotaItemResponse account = service.getAccount("CS1001");
        assertEquals(150_000L, account.dailyQuota());
        assertEquals(1, service.listAdjustmentLogs("CS1001", null, null, 1, 20).total());
    }

    @Test
    void batchUpdatesQuotaAndRecordsOneLogForEachAccount() {
        TokenQuotaService service = buildService();

        service.batchUpdateDailyQuota(new BatchUpdateDailyQuotaRequest(
                List.of("CS1001", "CS1002"),
                160_000L,
                "统一上调演示额度",
                "admin-1",
                "管理员"
        ));

        assertEquals(160_000L, service.getAccount("CS1001").dailyQuota());
        assertEquals(160_000L, service.getAccount("CS1002").dailyQuota());
        assertEquals(2, service.listAdjustmentLogs(null, null, null, 1, 20).total());
    }

    @Test
    void returnsSummaryForDashboardCards() {
        TokenQuotaService service = buildService();

        assertEquals(3, service.getSummary().totalAccounts());
        assertEquals(1, service.getSummary().highUsageCount());
        assertEquals(1, service.getSummary().exceededCount());
    }

    @Test
    void recordsUsageAndMarksExceededWithoutBlockingFurtherCounting() {
        TokenQuotaService service = buildService();

        RecordTokenUsageResponse response = service.recordUsage(new RecordTokenUsageRequest(
                "CS1003",
                10_000L,
                1L,
                1L
        ));

        assertEquals("EXCEEDED_RECOMMENDED", response.statusCode());
        assertEquals(101_000L, response.usedTokens());
        assertEquals(21_000L, response.overageTokens());
    }

    private TokenQuotaService buildService() {
        TokenQuotaProperties properties = new TokenQuotaProperties();
        Clock clock = Clock.systemUTC();
        TokenQuotaCalculator calculator = new TokenQuotaCalculator(properties);
        return new TokenQuotaService(
                new InMemoryTokenQuotaRepository(clock),
                new InMemoryTokenQuotaAdjustmentLogRepository(),
                properties,
                calculator,
                clock
        );
    }
}
