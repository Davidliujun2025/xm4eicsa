package tokenmonitor;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Normalized quota response. Null numeric fields mean provider did not supply that concept. */
public record ProviderQuotaSnapshot(
        Provider provider,
        String quotaType,
        BigDecimal availableBalance,
        String currency,
        Map<String, Object> details,
        Instant fetchedAt) {
}
