package tokenmonitor;

public final class ProviderQuotaException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int upstreamStatus;

    public ProviderQuotaException(String message, int upstreamStatus) {
        super(message);
        this.upstreamStatus = upstreamStatus;
    }

    public int upstreamStatus() { return upstreamStatus; }
}
