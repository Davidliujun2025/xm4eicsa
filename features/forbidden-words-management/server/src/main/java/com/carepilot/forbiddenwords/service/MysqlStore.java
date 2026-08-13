package com.carepilot.forbiddenwords.service;

import com.carepilot.forbiddenwords.model.ForbiddenWord;
import com.carepilot.forbiddenwords.model.HitAuditLog;
import com.carepilot.forbiddenwords.model.OperationAuditLog;
import com.carepilot.forbiddenwords.model.Platform;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@Profile("mysql")
public class MysqlStore implements DataStore {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ForbiddenWord> wordRowMapper = (rs, rowNum) -> new ForbiddenWord(
            rs.getLong("id"),
            rs.getString("word"),
            Platform.valueOf(rs.getString("platform")),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getString("created_by")
    );

    private final RowMapper<OperationAuditLog> opRowMapper = (rs, rowNum) -> new OperationAuditLog(
            rs.getLong("id"),
            rs.getString("action"),
            rs.getString("operator"),
            rs.getString("operator_ip"),
            rs.getString("target_word"),
            Platform.valueOf(rs.getString("platform")),
            rs.getTimestamp("operation_time").toLocalDateTime()
    );

    private final RowMapper<HitAuditLog> hitRowMapper = (rs, rowNum) -> new HitAuditLog(
            rs.getLong("id"),
            rs.getString("actor"),
            Platform.valueOf(rs.getString("platform")),
            rs.getString("source_type"),
            rs.getString("content"),
            rs.getString("hit_word"),
            rs.getString("action"),
            rs.getTimestamp("action_time").toLocalDateTime()
    );

    public MysqlStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ForbiddenWord> listWords() {
        return jdbcTemplate.query("SELECT id, word, platform, created_at, created_by FROM forbidden_word", wordRowMapper);
    }

    @Override
    public long countWords() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM forbidden_word", Long.class);
        return Objects.requireNonNullElse(count, 0L);
    }

    @Override
    public long countCoveredPlatforms() {
        Long allPlatformWords = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM forbidden_word WHERE platform = 'ALL'", Long.class);
        if (Objects.requireNonNullElse(allPlatformWords, 0L) > 0) {
            return java.util.Arrays.stream(Platform.values())
                    .filter(platform -> platform != Platform.ALL)
                    .count();
        }
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT platform) FROM forbidden_word WHERE platform <> 'ALL'",
                Long.class);
        return Objects.requireNonNullElse(count, 0L);
    }

    @Override
    public ForbiddenWord addWord(String word, Platform platform, String createdBy, LocalDateTime createdAt) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO forbidden_word(word, platform, created_at, created_by) VALUES(?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, word);
            ps.setString(2, platform.name());
            ps.setTimestamp(3, Timestamp.valueOf(createdAt));
            ps.setString(4, createdBy);
            return ps;
        }, keyHolder);
        Number id = keyHolder.getKey();
        return new ForbiddenWord(id == null ? null : id.longValue(), word, platform, createdAt, createdBy);
    }

    @Override
    public Optional<ForbiddenWord> removeWordById(Long id) {
        List<ForbiddenWord> rows = jdbcTemplate.query(
                "SELECT id, word, platform, created_at, created_by FROM forbidden_word WHERE id = ?",
                wordRowMapper,
                id
        );
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        ForbiddenWord target = rows.get(0);
        jdbcTemplate.update("DELETE FROM forbidden_word WHERE id = ?", id);
        return Optional.of(target);
    }

    @Override
    public List<OperationAuditLog> listOperationLogs() {
        return jdbcTemplate.query(
                "SELECT id, action, operator, operator_ip, target_word, platform, operation_time FROM operation_audit_log",
                opRowMapper
        );
    }

    @Override
    public OperationAuditLog addOperationLog(String action, String operator, String operatorIp, String targetWord, Platform platform, LocalDateTime operationTime) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO operation_audit_log(action, operator, operator_ip, target_word, platform, operation_time) VALUES(?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, action);
            ps.setString(2, operator);
            ps.setString(3, operatorIp);
            ps.setString(4, targetWord);
            ps.setString(5, platform.name());
            ps.setTimestamp(6, Timestamp.valueOf(operationTime));
            return ps;
        }, keyHolder);
        Number id = keyHolder.getKey();
        return new OperationAuditLog(id == null ? null : id.longValue(), action, operator, operatorIp, targetWord, platform, operationTime);
    }

    @Override
    public List<HitAuditLog> listHitLogs() {
        return jdbcTemplate.query(
                "SELECT id, actor, platform, source_type, content, hit_word, action, action_time FROM hit_audit_log",
                hitRowMapper
        );
    }

    @Override
    public HitAuditLog addHitLog(String actor, Platform platform, String sourceType, String content, String hitWord, String action, LocalDateTime actionTime) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO hit_audit_log(actor, platform, source_type, content, hit_word, action, action_time) VALUES(?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, actor);
            ps.setString(2, platform.name());
            ps.setString(3, sourceType);
            ps.setString(4, content);
            ps.setString(5, hitWord);
            ps.setString(6, action);
            ps.setTimestamp(7, Timestamp.valueOf(actionTime));
            return ps;
        }, keyHolder);
        Number id = keyHolder.getKey();
        return new HitAuditLog(id == null ? null : id.longValue(), actor, platform, sourceType, content, hitWord, action, actionTime);
    }

    @Override
    public long countActorHitsOnDate(String actor, LocalDateTime dateTime) {
        LocalDateTime start = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM hit_audit_log WHERE actor = ? AND action_time >= ? AND action_time < ?",
                Long.class,
                actor,
                Timestamp.valueOf(start),
                Timestamp.valueOf(end)
        );
        return Objects.requireNonNullElse(count, 0L);
    }

    @Override
    public long countHitsOnDate(LocalDateTime dateTime) {
        LocalDateTime start = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM hit_audit_log WHERE action_time >= ? AND action_time < ?",
                Long.class,
                Timestamp.valueOf(start),
                Timestamp.valueOf(end)
        );
        return Objects.requireNonNullElse(count, 0L);
    }
}
