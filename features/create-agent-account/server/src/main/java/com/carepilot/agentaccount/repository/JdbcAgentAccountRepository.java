package com.carepilot.agentaccount.repository;

import com.carepilot.agentaccount.model.AgentAccount;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class JdbcAgentAccountRepository
        implements AgentAccountRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AgentAccount> rowMapper =
            (resultSet, rowNum) -> {
                AgentAccount account = new AgentAccount();

                account.setUserId(resultSet.getLong("user_id"));
                account.setName(resultSet.getString("name"));
                account.setPhone(resultSet.getString("phone"));
                account.setEmail(resultSet.getString("email"));
                account.setPasswordHash(
                        resultSet.getString("password_hash")
                );
                account.setRole(resultSet.getString("role"));
                account.setStatus(resultSet.getString("status"));
                account.setCreatedAt(
                        resultSet.getTimestamp("created_at")
                                .toLocalDateTime()
                );
                account.setUpdatedAt(
                        resultSet.getTimestamp("updated_at")
                                .toLocalDateTime()
                );

                return account;
            };

    public JdbcAgentAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AgentAccount save(AgentAccount account) {
        String sql = """
                INSERT INTO agent_accounts
                    (name, phone, email, password_hash, role, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                account.getName(),
                account.getPhone(),
                account.getEmail(),
                account.getPasswordHash(),
                account.getRole(),
                account.getStatus()
        );

        return findByPhone(account.getPhone())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "账号创建后无法读取"
                        )
                );
    }

    @Override
    public Optional<AgentAccount> findByPhone(String phone) {
        String sql = """
                SELECT user_id, name, phone, email, password_hash,
                       role, status, created_at, updated_at
                FROM agent_accounts
                WHERE phone = ?
                """;

        return jdbcTemplate.query(sql, rowMapper, phone)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<AgentAccount> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        String sql = """
                SELECT user_id, name, phone, email, password_hash,
                       role, status, created_at, updated_at
                FROM agent_accounts
                WHERE email = ?
                """;

        return jdbcTemplate.query(sql, rowMapper, email)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<AgentAccount> findById(Long userId) {
        String sql = """
                SELECT user_id, name, phone, email, password_hash,
                       role, status, created_at, updated_at
                FROM agent_accounts
                WHERE user_id = ?
                """;

        return jdbcTemplate.query(sql, rowMapper, userId)
                .stream()
                .findFirst();
    }
}