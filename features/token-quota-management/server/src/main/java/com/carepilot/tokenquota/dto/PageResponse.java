package com.carepilot.tokenquota.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> records,
        long total,
        int page,
        int pageSize,
        long totalPages
) {
}
