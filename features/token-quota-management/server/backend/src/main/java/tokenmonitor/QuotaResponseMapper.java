package tokenmonitor;


import java.time.Instant;
import java.util.Map;

@FunctionalInterface
interface QuotaResponseMapper {
    ProviderQuotaSnapshot map(Provider provider, Map<String, Object> body, Instant fetchedAt);
}
