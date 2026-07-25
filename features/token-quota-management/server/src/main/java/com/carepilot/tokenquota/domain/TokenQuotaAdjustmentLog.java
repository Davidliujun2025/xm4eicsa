package com.carepilot.tokenquota.domain;

import java.time.LocalDateTime;

public record TokenQuotaAdjustmentLog(
        Long id,
        String accountNo,
        String accountName,
        long beforeQuota,
        long afterQuota,
        String operatorId,
        String operatorName,
        String reason,
        LocalDateTime adjustedAt
) {
}
