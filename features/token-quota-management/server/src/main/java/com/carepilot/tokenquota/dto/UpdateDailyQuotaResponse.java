package com.carepilot.tokenquota.dto;

import java.time.LocalDateTime;

public record UpdateDailyQuotaResponse(
        String accountNo,
        long beforeQuota,
        long afterQuota,
        LocalDateTime adjustedAt
) {
}
