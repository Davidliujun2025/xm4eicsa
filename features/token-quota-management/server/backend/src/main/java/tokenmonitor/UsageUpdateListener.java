package tokenmonitor;


@FunctionalInterface
public interface UsageUpdateListener {
    void onUpdate(TokenUsageSummary summary);
}
