package tokenmonitor;


/** Evidence-backed capability; documentationUrl points to the provider's official page. */
public record ProviderCapability(
        Provider provider,
        CapabilityStatus accountBalance,
        CapabilityStatus tokenPlanRemaining,
        CapabilityStatus historicalUsage,
        String note,
        String documentationUrl) {
}
