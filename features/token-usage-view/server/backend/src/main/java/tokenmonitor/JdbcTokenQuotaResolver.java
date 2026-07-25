package tokenmonitor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Reads the active per-user daily quota from MySQL and falls back to the configured default. */
public final class JdbcTokenQuotaResolver implements TokenQuotaResolver {
    private static final String SELECT_DAILY_LIMIT = """
            SELECT daily_token_limit
            FROM customer_token_quota
            WHERE user_id=? AND status='ACTIVE'
            LIMIT 1
            """;

    private final DataSource dataSource;
    private final long defaultDailyLimit;

    public JdbcTokenQuotaResolver(DataSource dataSource, long defaultDailyLimit) {
        if (dataSource == null) throw new IllegalArgumentException("dataSource is required");
        if (defaultDailyLimit < 0) throw new IllegalArgumentException("defaultDailyLimit must be >= 0");
        this.dataSource = dataSource;
        this.defaultDailyLimit = defaultDailyLimit;
    }

    @Override
    public Long dailyLimit(String userId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_DAILY_LIMIT)) {
            statement.setString(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong("daily_token_limit") : defaultDailyLimit;
            }
        } catch (SQLException error) {
            throw new IllegalStateException("failed to query customer token quota", error);
        }
    }
}
