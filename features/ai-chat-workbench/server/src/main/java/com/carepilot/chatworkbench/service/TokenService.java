package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import com.carepilot.chatworkbench.repository.AiDialogStepRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Workbench token facade backed by the same quota and usage tables used by the
 * quota-management and token-statistics pages.
 */
@Slf4j
@Service
public class TokenService {

    private final AiDialogStepRecordRepository stepRecordRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final long defaultDailyLimit;
    private final ZoneId zoneId;
    private final Clock clock;

    public TokenService(
            AiDialogStepRecordRepository stepRecordRepository,
            JdbcTemplate jdbcTemplate,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.token-monitor.default-daily-limit:0}") long defaultDailyLimit,
            @Value("${app.token-monitor.zone:Asia/Shanghai}") String zone
    ) {
        this.stepRecordRepository = stepRecordRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.eventPublisher = eventPublisher;
        this.defaultDailyLimit = Math.max(0, defaultDailyLimit);
        this.zoneId = ZoneId.of(zone);
        this.clock = Clock.systemUTC();
    }

    public Integer getTodayUsedTokens(String customerId) {
        return toInteger(sumToday(customerId, "total_tokens"));
    }

    public Integer getTodayPromptTokens(String customerId) {
        return toInteger(sumToday(customerId, "input_tokens"));
    }

    public Integer getTodayCompletionTokens(String customerId) {
        return toInteger(sumToday(customerId, "output_tokens"));
    }

    public Integer getSessionUsedTokens(Long sessionTaskId) {
        return toInteger(stepRecordRepository.sumTotalTokensBySessionTaskId(sessionTaskId));
    }

    public boolean isTokenQuotaExceeded(String customerId) {
        long dailyLimit = getDailyLimit(customerId);
        return dailyLimit <= 0 || getTodayUsedTokens(customerId) >= dailyLimit;
    }

    public Integer getDailyLimit(String customerId) {
        Long configured = jdbcTemplate.query(
                """
                SELECT daily_token_limit
                FROM customer_token_quota
                WHERE user_id = ? AND status = 'ACTIVE'
                LIMIT 1
                """,
                rs -> rs.next() ? rs.getLong("daily_token_limit") : null,
                customerId);
        return toInteger(configured == null ? defaultDailyLimit : configured);
    }

    public long getCumulativeUsedTokens(String customerId) {
        Long value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_tokens), 0) FROM token_usage_event WHERE user_id = ?",
                Long.class, customerId);
        return value == null ? 0 : Math.max(0, value);
    }

    public void recordEvaluationUsage(String customerId, String conversationId,
                                      DeepSeekClient.DeepSeekResult result) {
        String requestId = nonBlank(result.requestId(),
                "evaluation-" + conversationId + "-" + java.util.UUID.randomUUID());
        long promptTokens = nonNegative(result.promptTokens());
        long completionTokens = nonNegative(result.completionTokens());
        long totalTokens = Math.max(nonNegative(result.totalTokens()), promptTokens + completionTokens);
        int inserted = jdbcTemplate.update("""
                INSERT INTO token_usage_event (
                    idempotency_key, user_id, conversation_id, request_id, provider, model,
                    input_content_length, output_content_length, input_tokens, output_tokens,
                    cached_input_tokens, total_tokens, response_time_ms, status, occurred_at,
                    provider_reported_cost, cost_currency)
                VALUES (?, ?, ?, ?, 'DEEPSEEK', ?, 0, 0, ?, ?, 0, ?, 0, 'SUCCEEDED', NOW(6), 0, 'UNKNOWN')
                ON DUPLICATE KEY UPDATE idempotency_key = idempotency_key
                """,
                "deepseek:" + requestId, customerId, conversationId, requestId,
                nonBlank(result.model(), "deepseek-chat"), promptTokens, completionTokens, totalTokens);
        if (inserted > 0) eventPublisher.publishEvent(new TokenUsageRecordedEvent(customerId));
    }

    public Double getUsagePercent(String customerId) {
        int dailyLimit = getDailyLimit(customerId);
        if (dailyLimit <= 0) {
            return 0.0;
        }
        return (getTodayUsedTokens(customerId).doubleValue() / dailyLimit) * 100;
    }

    /** Records one successful DeepSeek invocation in the shared usage ledger. */
    public void recordAiUsage(AiDialogStepRecord record, String conversationId, String model) {
        String requestId = nonBlank(record.getTraceId(), "ai-dialog-step-" + record.getRecordId());
        String idempotencyKey = "deepseek:" + requestId;
        long promptTokens = nonNegative(record.getPromptTokens());
        long completionTokens = nonNegative(record.getCompletionTokens());
        long totalTokens = Math.max(nonNegative(record.getTotalTokens()), promptTokens + completionTokens);
        Instant occurredAt = toInstant(record.getLlmReplyAt() != null
                ? record.getLlmReplyAt() : record.getTriggerAt());

        int inserted = jdbcTemplate.update("""
                INSERT INTO token_usage_event (
                    idempotency_key, user_id, conversation_id, request_id, provider, model,
                    input_content_length, output_content_length, input_tokens, output_tokens,
                    cached_input_tokens, total_tokens, response_time_ms, status, occurred_at,
                    provider_reported_cost, cost_currency)
                VALUES (?, ?, ?, ?, 'DEEPSEEK', ?, ?, ?, ?, ?, 0, ?, ?, 'SUCCEEDED', ?, 0, 'UNKNOWN')
                ON DUPLICATE KEY UPDATE idempotency_key = idempotency_key
                """,
                idempotencyKey,
                String.valueOf(record.getOperatorId()),
                conversationId,
                requestId,
                nonBlank(model, "deepseek-chat"),
                length(record.getCustomerDialog()),
                length(record.getAiContent()),
                promptTokens,
                completionTokens,
                totalTokens,
                responseTimeMillis(record),
                Timestamp.from(occurredAt));
        if (inserted > 0) {
            eventPublisher.publishEvent(new TokenUsageRecordedEvent(String.valueOf(record.getOperatorId())));
        }
    }

    /**
     * Imports successful historical workbench calls once. The NOT EXISTS guard
     * also prevents duplication when a request was already recorded live.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void backfillWorkbenchUsage() {
        try {
            int inserted = jdbcTemplate.update("""
                    INSERT INTO token_usage_event (
                        idempotency_key, user_id, conversation_id, request_id, provider, model,
                        input_content_length, output_content_length, input_tokens, output_tokens,
                        cached_input_tokens, total_tokens, response_time_ms, status, occurred_at,
                        provider_reported_cost, cost_currency)
                    SELECT CONCAT('ai-dialog-step:', r.record_id),
                           CAST(r.operator_id AS CHAR),
                           c.conversation_id,
                           COALESCE(NULLIF(r.trace_id, ''), CONCAT('ai-dialog-step-', r.record_id)),
                           'DEEPSEEK',
                           'deepseek-chat',
                           CHAR_LENGTH(COALESCE(r.customer_dialog, '')),
                           CHAR_LENGTH(COALESCE(r.ai_content, '')),
                           GREATEST(COALESCE(r.prompt_tokens, 0), 0),
                           GREATEST(COALESCE(r.completion_tokens, 0), 0),
                           0,
                           GREATEST(COALESCE(r.total_tokens, 0),
                                    COALESCE(r.prompt_tokens, 0) + COALESCE(r.completion_tokens, 0)),
                           GREATEST(COALESCE(TIMESTAMPDIFF(MICROSECOND, r.trigger_at, r.llm_reply_at) DIV 1000, 0), 0),
                           'SUCCEEDED',
                           COALESCE(r.llm_reply_at, r.trigger_at, r.create_time),
                           0,
                           'UNKNOWN'
                    FROM ai_dialog_step_record r
                    LEFT JOIN conversation c ON c.id = r.session_task_id
                    WHERE r.generate_status = 1
                      AND r.is_delete = 0
                      AND r.operator_id IS NOT NULL
                      AND COALESCE(r.total_tokens, 0) > 0
                      AND NOT EXISTS (
                          SELECT 1
                          FROM token_usage_event e
                          WHERE e.user_id = CAST(r.operator_id AS CHAR)
                            AND e.request_id = COALESCE(NULLIF(r.trace_id, ''), CONCAT('ai-dialog-step-', r.record_id))
                      )
                    """);
            if (inserted > 0) {
                log.info("Backfilled {} workbench token usage events", inserted);
            }
        } catch (RuntimeException exception) {
            log.warn("Unable to backfill workbench token usage events; live recording remains enabled", exception);
        }
    }

    private long sumToday(String customerId, String tokenColumn) {
        if (!tokenColumn.equals("total_tokens")
                && !tokenColumn.equals("input_tokens")
                && !tokenColumn.equals("output_tokens")) {
            throw new IllegalArgumentException("Unsupported token column");
        }
        Instant from = LocalDate.now(clock.withZone(zoneId)).atStartOfDay(zoneId).toInstant();
        Instant to = from.atZone(zoneId).plusDays(1).toInstant();
        Long value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(" + tokenColumn + "), 0) FROM token_usage_event "
                        + "WHERE user_id = ? AND occurred_at >= ? AND occurred_at < ?",
                Long.class,
                customerId,
                Timestamp.from(from),
                Timestamp.from(to));
        return value == null ? 0 : value;
    }

    private Instant toInstant(java.time.LocalDateTime value) {
        return (value == null ? java.time.LocalDateTime.now(zoneId) : value)
                .atZone(zoneId).toInstant();
    }

    private long responseTimeMillis(AiDialogStepRecord record) {
        if (record.getTriggerAt() == null || record.getLlmReplyAt() == null) {
            return 0;
        }
        return Math.max(0, java.time.Duration.between(record.getTriggerAt(), record.getLlmReplyAt()).toMillis());
    }

    private long nonNegative(Integer value) {
        return value == null ? 0 : Math.max(0, value.longValue());
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }

    private String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Integer toInteger(Long value) {
        return toInteger(value == null ? 0L : value.longValue());
    }

    private Integer toInteger(long value) {
        return Math.toIntExact(Math.max(0, value));
    }
}
