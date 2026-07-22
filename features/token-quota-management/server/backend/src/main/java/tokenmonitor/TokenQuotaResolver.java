package tokenmonitor;

/** Integration port implemented by the Token quota-management module. */
@FunctionalInterface
public interface TokenQuotaResolver {
    /** Returns the user's daily quota; zero or null means not configured. */
    Long dailyLimit(String userId);
}
