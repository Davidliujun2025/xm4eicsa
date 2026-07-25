package com.carepilot.tokenquota.dto;

import java.util.List;

public record BatchUpdateDailyQuotaResponse(
        int requestedCount,
        int successCount,
        List<UpdateDailyQuotaResponse> results
) {
}
