package tokenmonitor;

import java.time.Instant;
import java.util.List;

public record TrendSeries(
        String userId,
        Instant from,
        Instant to,
        List<TrendPoint> items,
        boolean empty) {
}
