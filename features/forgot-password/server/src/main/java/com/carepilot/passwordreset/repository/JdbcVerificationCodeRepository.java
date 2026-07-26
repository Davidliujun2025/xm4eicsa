package com.carepilot.passwordreset.repository;

import com.carepilot.passwordreset.domain.VerificationCodeRecord;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcVerificationCodeRepository implements VerificationCodeRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcVerificationCodeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<VerificationCodeRecord> findLatestUnconsumed(String resetToken, String phone) {
        List<VerificationCodeRecord> records = jdbcTemplate.query("""
                SELECT id, code_hash, expires_at, next_allowed_at
                FROM password_reset_sms_code
                WHERE reset_token = ?
                  AND phone = ?
                  AND consumed_at IS NULL
                ORDER BY id DESC
                LIMIT 1
                """, (rs, rowNum) -> new VerificationCodeRecord(
                rs.getLong("id"),
                rs.getString("code_hash"),
                rs.getTimestamp("expires_at").toInstant(),
                rs.getTimestamp("next_allowed_at").toInstant()
        ), resetToken, phone);
        return records.stream().findFirst();
    }

    @Override
    public void save(String resetToken, Long accountId, String account, String phone, String codeHash,
                     Instant expiresAt, Instant nextAllowedAt) {
        jdbcTemplate.update("""
                INSERT INTO password_reset_sms_code
                    (reset_token, account_id, account, phone, code_hash, expires_at, next_allowed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                resetToken,
                accountId,
                account,
                phone,
                codeHash,
                Timestamp.from(expiresAt),
                Timestamp.from(nextAllowedAt)
        );
    }

    @Override
    public void consume(Long id) {
        jdbcTemplate.update("""
                UPDATE password_reset_sms_code
                SET consumed_at = CURRENT_TIMESTAMP(6)
                WHERE id = ? AND consumed_at IS NULL
                """, id);
    }

    @Override
    public void cleanupExpired(Instant now) {
        jdbcTemplate.update("""
                UPDATE password_reset_sms_code
                SET consumed_at = CURRENT_TIMESTAMP(6)
                WHERE consumed_at IS NULL
                  AND expires_at < ?
                """, Timestamp.from(now));
    }
}
