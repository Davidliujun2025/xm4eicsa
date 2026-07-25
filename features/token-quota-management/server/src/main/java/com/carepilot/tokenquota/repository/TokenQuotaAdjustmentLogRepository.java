package com.carepilot.tokenquota.repository;

import com.carepilot.tokenquota.domain.TokenQuotaAdjustmentLog;
import java.time.LocalDateTime;
import java.util.List;

public interface TokenQuotaAdjustmentLogRepository {
    TokenQuotaAdjustmentLog save(TokenQuotaAdjustmentLog log);

    List<TokenQuotaAdjustmentLog> findLogs(String accountNo, LocalDateTime startTime, LocalDateTime endTime);
}
