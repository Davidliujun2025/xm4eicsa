package tokenmonitor;

import java.util.Locale;

public enum CallStatus {
    SUCCEEDED,
    FAILED,
    CANCELLED,
    TIMEOUT;

    public static CallStatus parse(String value) {
        if (value == null) return FAILED;
        try { return valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return FAILED; }
    }
}
