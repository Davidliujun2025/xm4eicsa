package com.carepilot.passwordreset.repository;

import com.carepilot.passwordreset.domain.AccountRecord;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAccountRepository implements AccountRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<AccountRecord> findCustomerServiceByUsername(String username) {
        return find("username", username);
    }

    @Override
    public Optional<AccountRecord> findCustomerServiceByPhone(String phone) {
        return find("phone", phone);
    }

    @Override
    public void updatePasswordHash(Long accountId, String passwordHash) {
        jdbcTemplate.update("""
                UPDATE sys_user
                SET password_hash = ?, updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """, passwordHash, accountId);
    }

    private Optional<AccountRecord> find(String column, String value) {
        String sql = """
                SELECT id, username AS account, phone, password_hash, status
                FROM sys_user
                WHERE %s = ?
                LIMIT 1
                """.formatted(column);
        List<AccountRecord> records = jdbcTemplate.query(sql, this::mapAccount, value);
        return records.stream().findFirst();
    }

    private AccountRecord mapAccount(ResultSet rs, int rowNum) throws SQLException {
        String account = rs.getString("account");
        return new AccountRecord(
                rs.getLong("id"),
                rs.getString("phone"),
                account,
                rs.getString("password_hash"),
                "ENABLED".equalsIgnoreCase(rs.getString("status"))
        );
    }
}
