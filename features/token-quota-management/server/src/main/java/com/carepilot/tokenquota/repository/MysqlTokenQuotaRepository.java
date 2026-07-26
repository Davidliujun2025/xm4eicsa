package com.carepilot.tokenquota.repository;

import com.carepilot.tokenquota.domain.TokenQuotaAccount;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("mysql")
public class MysqlTokenQuotaRepository implements TokenQuotaRepository {
    private static final String ACCOUNT_SELECT = """
            SELECT
                u.id AS id,
                u.account AS account_no,
                COALESCE(NULLIF(u.display_name, ''), u.account) AS account_name,
                COALESCE(q.daily_token_limit, 0) AS daily_quota,
                COALESCE(today_usage.used_tokens_today, 0) AS used_tokens_today,
                COALESCE(today_usage.ai_call_count_today, 0) AS ai_call_count_today,
                COALESCE(today_usage.business_count_today, 0) AS business_count_today,
                COALESCE(q.status, 'ACTIVE') AS quota_status,
                u.status AS user_status
            FROM customer_service_user u
            LEFT JOIN customer_token_quota q ON q.user_id = u.account
            LEFT JOIN (
                SELECT
                    user_id,
                    COALESCE(SUM(total_tokens), 0) AS used_tokens_today,
                    COUNT(*) AS ai_call_count_today,
                    COUNT(DISTINCT NULLIF(conversation_id, '')) AS business_count_today
                FROM token_usage_event
                WHERE occurred_at >= :dayStart
                  AND occurred_at < :dayEnd
                GROUP BY user_id
            ) today_usage ON today_usage.user_id = u.account
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public MysqlTokenQuotaRepository(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public List<TokenQuotaAccount> findAllAccounts() {
        return jdbc.query(
                ACCOUNT_SELECT + " ORDER BY u.account",
                todayParams(),
                (rs, rowNum) -> mapAccount(rs)
        );
    }

    @Override
    public Optional<TokenQuotaAccount> findByAccountNo(String accountNo) {
        MapSqlParameterSource params = todayParams()
                .addValue("accountNo", normalize(accountNo));
        List<TokenQuotaAccount> accounts = jdbc.query(
                ACCOUNT_SELECT + " WHERE UPPER(u.account) = :accountNo",
                params,
                (rs, rowNum) -> mapAccount(rs)
        );
        return accounts.stream().findFirst();
    }

    @Override
    public TokenQuotaAccount save(TokenQuotaAccount account) {
        String userId = findUserIdByAccountNo(account.accountNo());
        String status = account.enabled() ? "ACTIVE" : "DISABLED";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("dailyQuota", account.dailyQuota())
                .addValue("status", status)
                .addValue("updatedBy", "token-quota-admin");

        int updated = jdbc.update("""
                UPDATE customer_token_quota
                SET daily_token_limit = :dailyQuota,
                    status = :status,
                    updated_by = :updatedBy,
                    updated_at = CURRENT_TIMESTAMP(3)
                WHERE user_id = :userId
                """, params);

        if (updated == 0) {
            jdbc.update("""
                    INSERT INTO customer_token_quota (
                        user_id,
                        daily_token_limit,
                        status,
                        updated_by,
                        created_at,
                        updated_at
                    ) VALUES (
                        :userId,
                        :dailyQuota,
                        :status,
                        :updatedBy,
                        CURRENT_TIMESTAMP(3),
                        CURRENT_TIMESTAMP(3)
                    )
                    """, params);
        }

        return findByAccountNo(account.accountNo()).orElse(account);
    }

    @Override
    public TokenQuotaAccount recordUsage(
            TokenQuotaAccount updatedAccount,
            long consumedTokens,
            long aiCallCount,
            long businessCount,
            LocalDateTime occurredAt
    ) {
        String userId = findUserIdByAccountNo(updatedAccount.accountNo());
        String requestId = "manual-" + UUID.randomUUID();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("idempotencyKey", requestId)
                .addValue("userId", userId)
                .addValue("conversationId", businessCount > 0 ? "manual-business-" + UUID.randomUUID() : null)
                .addValue("requestId", requestId)
                .addValue("provider", "INTERNAL")
                .addValue("model", "manual-usage-record")
                .addValue("inputContentLength", 0)
                .addValue("outputContentLength", 0)
                .addValue("inputTokens", consumedTokens)
                .addValue("outputTokens", 0)
                .addValue("cachedInputTokens", 0)
                .addValue("totalTokens", consumedTokens)
                .addValue("responseTimeMs", 0)
                .addValue("status", "SUCCEEDED")
                .addValue("occurredAt", occurredAt)
                .addValue("providerReportedCost", BigDecimal.ZERO)
                .addValue("costCurrency", "CNY");

        jdbc.update("""
                INSERT INTO token_usage_event (
                    idempotency_key,
                    user_id,
                    conversation_id,
                    request_id,
                    provider,
                    model,
                    input_content_length,
                    output_content_length,
                    input_tokens,
                    output_tokens,
                    cached_input_tokens,
                    total_tokens,
                    response_time_ms,
                    status,
                    occurred_at,
                    provider_reported_cost,
                    cost_currency,
                    created_at
                ) VALUES (
                    :idempotencyKey,
                    :userId,
                    :conversationId,
                    :requestId,
                    :provider,
                    :model,
                    :inputContentLength,
                    :outputContentLength,
                    :inputTokens,
                    :outputTokens,
                    :cachedInputTokens,
                    :totalTokens,
                    :responseTimeMs,
                    :status,
                    :occurredAt,
                    :providerReportedCost,
                    :costCurrency,
                    CURRENT_TIMESTAMP(3)
                )
                """, params);

        return findByAccountNo(updatedAccount.accountNo()).orElse(updatedAccount);
    }

    private MapSqlParameterSource todayParams() {
        LocalDate today = LocalDate.now(clock);
        return new MapSqlParameterSource()
                .addValue("dayStart", today.atStartOfDay())
                .addValue("dayEnd", today.plusDays(1).atStartOfDay());
    }

    private TokenQuotaAccount mapAccount(java.sql.ResultSet rs) throws java.sql.SQLException {
        String quotaStatus = rs.getString("quota_status");
        String userStatus = rs.getString("user_status");
        boolean enabled = !"DISABLED".equalsIgnoreCase(quotaStatus)
                && !"DISABLED".equalsIgnoreCase(userStatus);

        return new TokenQuotaAccount(
                rs.getLong("id"),
                rs.getString("account_no"),
                rs.getString("account_name"),
                rs.getLong("daily_quota"),
                rs.getLong("used_tokens_today"),
                rs.getLong("ai_call_count_today"),
                rs.getLong("business_count_today"),
                LocalDate.now(clock),
                enabled
        );
    }

    private String findUserIdByAccountNo(String accountNo) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("accountNo", normalize(accountNo));
        List<String> ids = jdbc.query(
                "SELECT account FROM customer_service_user WHERE UPPER(account) = :accountNo",
                params,
                (rs, rowNum) -> rs.getString("account")
        );
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("客服账号不存在: " + accountNo);
        }
        return ids.get(0);
    }

    private String normalize(String accountNo) {
        return accountNo == null ? "" : accountNo.trim().toUpperCase();
    }
}
