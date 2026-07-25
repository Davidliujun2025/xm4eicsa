package com.carepilot.tokenquota.service;

import com.carepilot.tokenquota.config.TokenQuotaProperties;
import com.carepilot.tokenquota.dto.PageResponse;
import java.util.List;

public final class PageUtils {
    private PageUtils() {
    }

    public static <T> PageResponse<T> page(List<T> records, Integer page, Integer pageSize, TokenQuotaProperties properties) {
        int normalizedPage = page == null || page < 1 ? 1 : page;
        int normalizedPageSize = pageSize == null || pageSize < 1 ? properties.getDefaultPageSize() : pageSize;
        normalizedPageSize = Math.min(normalizedPageSize, properties.getMaxPageSize());

        int fromIndex = Math.min((normalizedPage - 1) * normalizedPageSize, records.size());
        int toIndex = Math.min(fromIndex + normalizedPageSize, records.size());
        List<T> pageRecords = records.subList(fromIndex, toIndex);
        long totalPages = records.isEmpty() ? 0 : (records.size() + normalizedPageSize - 1L) / normalizedPageSize;

        return new PageResponse<>(pageRecords, records.size(), normalizedPage, normalizedPageSize, totalPages);
    }
}
