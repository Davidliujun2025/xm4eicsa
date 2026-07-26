package com.carepilot.tokenquota.dto;

import java.time.LocalDateTime;

public record AdjustmentLogResponse(
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
