package com.carepilot.tokenquota.repository;

import com.carepilot.tokenquota.domain.TokenQuotaAdjustmentLog;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@Profile("memory")
public class InMemoryTokenQuotaAdjustmentLogRepository implements TokenQuotaAdjustmentLogRepository {
    private final AtomicLong idGenerator = new AtomicLong(0);
    private final CopyOnWriteArrayList<TokenQuotaAdjustmentLog> logs = new CopyOnWriteArrayList<>();

    @Override
    public TokenQuotaAdjustmentLog save(TokenQuotaAdjustmentLog log) {
        TokenQuotaAdjustmentLog saved = new TokenQuotaAdjustmentLog(
                idGenerator.incrementAndGet(),
                log.accountNo(),
                log.accountName(),
                log.beforeQuota(),
                log.afterQuota(),
                log.operatorId(),
                log.operatorName(),
                log.reason(),
                log.adjustedAt()
        );
        logs.add(saved);
        return saved;
    }

    @Override
    public List<TokenQuotaAdjustmentLog> findLogs(String accountNo, LocalDateTime startTime, LocalDateTime endTime) {
        String normalizedAccountNo = accountNo == null ? "" : accountNo.trim().toUpperCase();
        return logs.stream()
                .filter(log -> !StringUtils.hasText(normalizedAccountNo)
                        || log.accountNo().equalsIgnoreCase(normalizedAccountNo))
                .filter(log -> startTime == null || !log.adjustedAt().isBefore(startTime))
                .filter(log -> endTime == null || !log.adjustedAt().isAfter(endTime))
                .sorted(Comparator.comparing(TokenQuotaAdjustmentLog::adjustedAt).reversed())
                .toList();
    }
}
