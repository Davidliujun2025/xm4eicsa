package com.carepilot.tokenquota.repository;

import com.carepilot.tokenquota.domain.TokenQuotaAdjustmentLog;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@Profile("mysql")
public class MysqlTokenQuotaAdjustmentLogRepository implements TokenQuotaAdjustmentLogRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public MysqlTokenQuotaAdjustmentLogRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public TokenQuotaAdjustmentLog save(TokenQuotaAdjustmentLog log) {
        AccountIdentity identity = findAccountIdentity(log.accountNo());
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("quotaId", identity.quotaId())
                .addValue("userId", identity.userId())
                .addValue("accountNo", log.accountNo())
                .addValue("accountName", log.accountName())
                .addValue("beforeQuota", log.beforeQuota())
                .addValue("afterQuota", log.afterQuota())
                .addValue("operatorId", log.operatorId())
                .addValue("operatorName", log.operatorName())
                .addValue("reason", log.reason())
                .addValue("adjustedAt", log.adjustedAt());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update("""
                INSERT INTO token_quota_adjustment_log (
                    quota_id,
                    user_id,
                    account_no,
                    account_name,
                    before_daily_token_limit,
                    after_daily_token_limit,
                    operator_id,
                    operator_name,
                    reason,
                    adjusted_at,
                    created_at
                ) VALUES (
                    :quotaId,
                    :userId,
                    :accountNo,
                    :accountName,
                    :beforeQuota,
                    :afterQuota,
                    :operatorId,
                    :operatorName,
                    :reason,
                    :adjustedAt,
                    CURRENT_TIMESTAMP(3)
                )
                """, params, keyHolder);

        Number id = keyHolder.getKey();
        return new TokenQuotaAdjustmentLog(
                id == null ? log.id() : id.longValue(),
                log.accountNo(),
                log.accountName(),
                log.beforeQuota(),
                log.afterQuota(),
                log.operatorId(),
                log.operatorName(),
                log.reason(),
                log.adjustedAt()
        );
    }

    @Override
    public List<TokenQuotaAdjustmentLog> findLogs(String accountNo, LocalDateTime startTime, LocalDateTime endTime) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    id,
                    account_no,
                    account_name,
                    before_daily_token_limit,
                    after_daily_token_limit,
                    operator_id,
                    operator_name,
                    reason,
                    adjusted_at
                FROM token_quota_adjustment_log
                WHERE 1 = 1
                """);
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (StringUtils.hasText(accountNo)) {
            sql.append(" AND UPPER(account_no) = :accountNo");
            params.addValue("accountNo", accountNo.trim().toUpperCase());
        }
        if (startTime != null) {
            sql.append(" AND adjusted_at >= :startTime");
            params.addValue("startTime", startTime);
        }
        if (endTime != null) {
            sql.append(" AND adjusted_at <= :endTime");
            params.addValue("endTime", endTime);
        }
        sql.append(" ORDER BY adjusted_at DESC, id DESC");

        return jdbc.query(sql.toString(), params, (rs, rowNum) -> mapLog(rs));
    }

    private TokenQuotaAdjustmentLog mapLog(ResultSet rs) throws SQLException {
        return new TokenQuotaAdjustmentLog(
                rs.getLong("id"),
                rs.getString("account_no"),
                rs.getString("account_name"),
                rs.getLong("before_daily_token_limit"),
                rs.getLong("after_daily_token_limit"),
                rs.getString("operator_id"),
                rs.getString("operator_name"),
                rs.getString("reason"),
                rs.getObject("adjusted_at", LocalDateTime.class)
        );
    }

    private AccountIdentity findAccountIdentity(String accountNo) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("accountNo", accountNo == null ? "" : accountNo.trim().toUpperCase());
        List<AccountIdentity> identities = jdbc.query("""
                SELECT
                    u.account AS user_id,
                    q.id AS quota_id
                FROM customer_service_user u
                LEFT JOIN customer_token_quota q ON q.user_id = u.account
                WHERE UPPER(u.account) = :accountNo
                """, params, (rs, rowNum) -> new AccountIdentity(
                rs.getString("user_id"),
                rs.getObject("quota_id", Long.class)
        ));
        if (identities.isEmpty()) {
            throw new IllegalArgumentException("客服账号不存在: " + accountNo);
        }
        return identities.get(0);
    }

    private record AccountIdentity(String userId, Long quotaId) {
    }
}
