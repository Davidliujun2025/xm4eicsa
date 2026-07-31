package com.carepilot.platform.token;

import com.acme.aicslogin.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/admin/token-quotas")
public class TokenQuotaAdminController {

    private final JdbcTemplate jdbcTemplate;
    private final ZoneId zoneId;
    private final Clock clock;

    public TokenQuotaAdminController(
            JdbcTemplate jdbcTemplate,
            @org.springframework.beans.factory.annotation.Value("${app.token-monitor.zone:Asia/Shanghai}") String zone
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.zoneId = ZoneId.of(zone);
        this.clock = Clock.systemUTC();
    }

    @GetMapping
    public QuotaPage list(@RequestParam(defaultValue = "") String keyword) {
        Instant from = LocalDate.now(clock.withZone(zoneId)).atStartOfDay(zoneId).toInstant();
        Instant to = from.atZone(zoneId).plusDays(1).toInstant();
        String normalizedKeyword = "%" + keyword.trim() + "%";
        List<QuotaItem> items = jdbcTemplate.query("""
                SELECT u.id,
                       u.username,
                       u.status,
                       COALESCE(q.daily_token_limit, 0) AS daily_token_limit,
                       COALESCE(SUM(CASE WHEN e.occurred_at >= ? AND e.occurred_at < ?
                                         THEN e.total_tokens ELSE 0 END), 0) AS used_today
                FROM sys_user u
                LEFT JOIN customer_token_quota q
                       ON q.user_id = CAST(u.id AS CHAR) AND q.status = 'ACTIVE'
                LEFT JOIN token_usage_event e ON e.user_id = CAST(u.id AS CHAR)
                WHERE u.role_type = 'CUSTOMER_SERVICE'
                  AND (? = '%%' OR u.username LIKE ?)
                GROUP BY u.id, u.username, u.status, q.daily_token_limit
                ORDER BY u.id DESC
                """,
                (rs, rowNum) -> new QuotaItem(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("status"),
                        rs.getLong("daily_token_limit"),
                        rs.getLong("used_today")),
                Timestamp.from(from),
                Timestamp.from(to),
                normalizedKeyword,
                normalizedKeyword);

        long totalQuota = items.stream().mapToLong(QuotaItem::dailyTokenLimit).sum();
        long usedToday = items.stream().mapToLong(QuotaItem::usedToday).sum();
        long highUsageCount = items.stream().filter(item -> item.usageRate() >= 0.8d).count();
        long exceededCount = items.stream().filter(item -> item.dailyTokenLimit() > 0
                && item.usedToday() > item.dailyTokenLimit()).count();
        return new QuotaPage(items, new QuotaSummary(
                items.size(), totalQuota, usedToday, highUsageCount, exceededCount));
    }

    @PatchMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @PathVariable long userId,
            @Valid @RequestBody UpdateQuotaRequest request,
            @AuthenticationPrincipal AuthenticatedUser operator
    ) {
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE id = ? AND role_type = 'CUSTOMER_SERVICE'",
                Integer.class,
                userId);
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "客服账号不存在");
        }
        jdbcTemplate.update("""
                INSERT INTO customer_token_quota
                    (user_id, daily_token_limit, status, updated_by, created_at, updated_at)
                VALUES (?, ?, 'ACTIVE', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE
                    daily_token_limit = VALUES(daily_token_limit),
                    status = 'ACTIVE',
                    updated_by = VALUES(updated_by),
                    updated_at = CURRENT_TIMESTAMP
                """,
                String.valueOf(userId),
                request.dailyTokenLimit(),
                operator.account());
    }

    public record QuotaPage(List<QuotaItem> items, QuotaSummary summary) {
    }

    public record QuotaSummary(
            long totalUsers,
            long totalQuota,
            long usedToday,
            long highUsageCount,
            long exceededCount
    ) {
    }

    public record QuotaItem(
            long userId,
            String username,
            String status,
            long dailyTokenLimit,
            long usedToday
    ) {
        public long remaining() {
            return dailyTokenLimit - usedToday;
        }

        public double usageRate() {
            return dailyTokenLimit == 0 ? 0d : (double) usedToday / dailyTokenLimit;
        }
    }

    public record UpdateQuotaRequest(@Min(0) long dailyTokenLimit, String reason) {
    }
}
