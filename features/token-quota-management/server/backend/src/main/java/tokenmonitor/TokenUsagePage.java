package tokenmonitor;

import java.util.List;

/** Stable page envelope. Page numbers are one-based for the frontend. */
public record TokenUsagePage(
        List<TokenUsageEvent> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean empty) {
}
