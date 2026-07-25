package tokenmonitor;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** MySQL 8 repository. Database primary key gives cross-instance idempotency. */
public final class JdbcTokenUsageRepository implements TokenUsageRepository {
    private static final String INSERT = """
            INSERT INTO token_usage_event (
              idempotency_key,user_id,conversation_id,request_id,provider,model,
              input_content_length,output_content_length,input_tokens,
              output_tokens,cached_input_tokens,total_tokens,response_time_ms,status,occurred_at,
              provider_reported_cost,cost_currency)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE idempotency_key=idempotency_key
            """;

    private final DataSource dataSource;

    public JdbcTokenUsageRepository(DataSource dataSource) { this.dataSource = dataSource; }

    @Override
    public boolean saveIfAbsent(TokenUsageEvent event) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {
            int i = 1;
            statement.setString(i++, event.idempotencyKey());
            statement.setString(i++, event.userId());
            statement.setString(i++, event.conversationId());
            statement.setString(i++, event.requestId());
            statement.setString(i++, event.provider().name());
            statement.setString(i++, event.model());
            statement.setLong(i++, event.inputContentLength());
            statement.setLong(i++, event.outputContentLength());
            statement.setLong(i++, event.inputTokens());
            statement.setLong(i++, event.outputTokens());
            statement.setLong(i++, event.cachedInputTokens());
            statement.setLong(i++, event.totalTokens());
            statement.setLong(i++, event.responseTimeMs());
            statement.setString(i++, event.status().name());
            statement.setTimestamp(i++, Timestamp.from(event.occurredAt()));
            statement.setBigDecimal(i++, event.providerReportedCost());
            statement.setString(i, event.costCurrency());
            return statement.executeUpdate() == 1;
        } catch (SQLException error) {
            throw new IllegalStateException("failed to insert token usage event", error);
        }
    }

    @Override
    public List<TokenUsageEvent> find(String userId, Instant fromInclusive, Instant toExclusive,
                                      Provider provider, String model, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT idempotency_key,user_id,conversation_id,request_id,provider,model,
                  input_content_length,output_content_length,input_tokens,
                  output_tokens,cached_input_tokens,total_tokens,response_time_ms,status,occurred_at,
                  provider_reported_cost,cost_currency
                FROM token_usage_event
                WHERE user_id=? AND occurred_at>=? AND occurred_at<?
                """);
        if (provider != null) sql.append(" AND provider=?");
        if (model != null && !model.isBlank()) sql.append(" AND model=?");
        sql.append(" ORDER BY occurred_at DESC,idempotency_key");
        if (limit > 0) sql.append(" LIMIT ?");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int i = 1;
            statement.setString(i++, userId);
            statement.setTimestamp(i++, Timestamp.from(fromInclusive));
            statement.setTimestamp(i++, Timestamp.from(toExclusive));
            if (provider != null) statement.setString(i++, provider.name());
            if (model != null && !model.isBlank()) statement.setString(i++, model);
            if (limit > 0) statement.setInt(i, Math.max(1, Math.min(limit, 1_000)));
            try (ResultSet result = statement.executeQuery()) {
                List<TokenUsageEvent> events = new ArrayList<>();
                while (result.next()) events.add(read(result));
                return List.copyOf(events);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("failed to query token usage events", error);
        }
    }

    @Override
    public List<TokenUsageEvent> findPage(String userId, Instant fromInclusive, Instant toExclusive,
                                          Provider provider, String model, int offset, int limit) {
        StringBuilder sql = selectSql(provider, model);
        sql.append(" ORDER BY occurred_at DESC,idempotency_key LIMIT ? OFFSET ?");
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int i = bindFilters(statement, userId, fromInclusive, toExclusive, provider, model);
            statement.setInt(i++, Math.max(1, Math.min(limit, 100)));
            statement.setInt(i, Math.max(0, offset));
            try (ResultSet result = statement.executeQuery()) {
                List<TokenUsageEvent> events = new ArrayList<>();
                while (result.next()) events.add(read(result));
                return List.copyOf(events);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("failed to query token usage page", error);
        }
    }

    @Override
    public long count(String userId, Instant fromInclusive, Instant toExclusive,
                      Provider provider, String model) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) FROM token_usage_event
                WHERE user_id=? AND occurred_at>=? AND occurred_at<?
                """);
        appendFilters(sql, provider, model);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindFilters(statement, userId, fromInclusive, toExclusive, provider, model);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        } catch (SQLException error) {
            throw new IllegalStateException("failed to count token usage events", error);
        }
    }

    @Override
    public Optional<TokenUsageEvent> findByRequestId(String userId, String requestId) {
        String sql = """
                SELECT idempotency_key,user_id,conversation_id,request_id,provider,model,
                  input_content_length,output_content_length,input_tokens,
                  output_tokens,cached_input_tokens,total_tokens,response_time_ms,status,occurred_at,
                  provider_reported_cost,cost_currency
                FROM token_usage_event
                WHERE user_id=? AND request_id=?
                ORDER BY occurred_at DESC
                LIMIT 1
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setString(2, requestId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("failed to query token usage detail", error);
        }
    }

    private static TokenUsageEvent read(ResultSet result) throws SQLException {
        return new TokenUsageEvent(
                result.getString("idempotency_key"), result.getString("user_id"),
                result.getString("conversation_id"), result.getString("request_id"),
                Provider.parse(result.getString("provider")), result.getString("model"),
                result.getLong("input_content_length"), result.getLong("output_content_length"),
                result.getLong("input_tokens"), result.getLong("output_tokens"),
                result.getLong("cached_input_tokens"), result.getLong("total_tokens"),
                result.getLong("response_time_ms"), CallStatus.parse(result.getString("status")),
                result.getTimestamp("occurred_at").toInstant(), result.getBigDecimal("provider_reported_cost"),
                result.getString("cost_currency"));
    }

    private static StringBuilder selectSql(Provider provider, String model) {
        StringBuilder sql = new StringBuilder("""
                SELECT idempotency_key,user_id,conversation_id,request_id,provider,model,
                  input_content_length,output_content_length,input_tokens,
                  output_tokens,cached_input_tokens,total_tokens,response_time_ms,status,occurred_at,
                  provider_reported_cost,cost_currency
                FROM token_usage_event
                WHERE user_id=? AND occurred_at>=? AND occurred_at<?
                """);
        appendFilters(sql, provider, model);
        return sql;
    }

    private static void appendFilters(StringBuilder sql, Provider provider, String model) {
        if (provider != null) sql.append(" AND provider=?");
        if (model != null && !model.isBlank()) sql.append(" AND model=?");
    }

    private static int bindFilters(PreparedStatement statement, String userId, Instant fromInclusive,
                                   Instant toExclusive, Provider provider, String model) throws SQLException {
        int i = 1;
        statement.setString(i++, userId);
        statement.setTimestamp(i++, Timestamp.from(fromInclusive));
        statement.setTimestamp(i++, Timestamp.from(toExclusive));
        if (provider != null) statement.setString(i++, provider.name());
        if (model != null && !model.isBlank()) statement.setString(i++, model);
        return i;
    }
}
