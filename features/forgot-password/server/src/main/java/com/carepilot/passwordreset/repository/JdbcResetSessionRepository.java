package com.carepilot.passwordreset.repository;

import com.carepilot.passwordreset.domain.ResetSession;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcResetSessionRepository implements ResetSessionRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcResetSessionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(ResetSession session) {
        jdbcTemplate.update("""
                INSERT INTO password_reset_session
                    (reset_token, account_id, account, expires_at)
                VALUES (?, ?, ?, ?)
                """,
                session.token(),
                session.accountId(),
                session.username(),
                Timestamp.from(session.expiresAt())
        );
    }

    @Override
    public Optional<ResetSession> findActiveByToken(String token, Instant now) {
        List<ResetSession> sessions = jdbcTemplate.query("""
                SELECT reset_token, account_id, account, expires_at
                FROM password_reset_session
                WHERE reset_token = ?
                  AND consumed_at IS NULL
                  AND expires_at >= ?
                LIMIT 1
                """, (rs, rowNum) -> new ResetSession(
                rs.getString("reset_token"),
                rs.getLong("account_id"),
                rs.getString("account"),
                rs.getTimestamp("expires_at").toInstant()
        ), token, Timestamp.from(now));
        return sessions.stream().findFirst();
    }

    @Override
    public void consume(String token) {
        jdbcTemplate.update("""
                UPDATE password_reset_session
                SET consumed_at = CURRENT_TIMESTAMP(6)
                WHERE reset_token = ? AND consumed_at IS NULL
                """, token);
    }

    @Override
    public void cleanupExpired(Instant now) {
        jdbcTemplate.update("""
                UPDATE password_reset_session
                SET consumed_at = CURRENT_TIMESTAMP(6)
                WHERE consumed_at IS NULL
                  AND expires_at < ?
                """, Timestamp.from(now));
    }
}
