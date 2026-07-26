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
        return findByAccount(username);
    }

    @Override
    public Optional<AccountRecord> findCustomerServiceByPhone(String phone) {
        return findByAccount(phone);
    }

    @Override
    public void updatePasswordHash(Long accountId, String passwordHash) {
        jdbcTemplate.update("""
                UPDATE customer_service_user
                SET password_hash = ?, updated_at = CURRENT_TIMESTAMP(6)
                WHERE id = ?
                """, passwordHash, accountId);
    }

    private Optional<AccountRecord> findByAccount(String account) {
        List<AccountRecord> records = jdbcTemplate.query("""
                SELECT id, account, password_hash, status
                FROM customer_service_user
                WHERE account = ?
                LIMIT 1
                """, this::mapAccount, account);
        return records.stream().findFirst();
    }

    private AccountRecord mapAccount(ResultSet rs, int rowNum) throws SQLException {
        String account = rs.getString("account");
        return new AccountRecord(
                rs.getLong("id"),
                account,
                account,
                rs.getString("password_hash"),
                "ACTIVE".equalsIgnoreCase(rs.getString("status"))
        );
    }
}
