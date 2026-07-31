package com.carepilot.agentaccount.repository;

import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.model.OperationLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcAgentAccountRepository implements AgentAccountRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<AgentAccount> accountRowMapper = (rs, rowNum) -> {
        AgentAccount account = new AgentAccount();
        account.setUserId(rs.getLong("user_id"));
        account.setName(rs.getString("name"));
        account.setPhone(rs.getString("phone"));
        account.setEmail(rs.getString("email"));
        account.setPasswordHash(rs.getString("password_hash"));
        account.setRole(fromDbRole(rs.getString("role")));
        account.setStatus(rs.getString("status"));
        account.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        account.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return account;
    };

    private final RowMapper<OperationLog> logRowMapper = (rs, rowNum) -> {
        OperationLog log = new OperationLog();
        log.setLogId(rs.getLong("log_id"));
        log.setUserId(rs.getLong("user_id"));
        log.setAction(rs.getString("action"));
        log.setDetail(rs.getString("detail"));
        log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return log;
    };

    public JdbcAgentAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AgentAccount save(AgentAccount account) {
        jdbcTemplate.update("""
                INSERT INTO sys_user
                    (username, phone, email, password_hash, role_type, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                account.getName(),
                account.getPhone(),
                account.getEmail(),
                account.getPasswordHash(),
                toDbRole(account.getRole()),
                account.getStatus());
        return findByPhone(account.getPhone())
                .orElseThrow(() -> new IllegalStateException("账号创建后无法读取"));
    }

    @Override
    public Optional<AgentAccount> findByPhone(String phone) {
        return jdbcTemplate.query(selectBase() + " WHERE phone = ?", accountRowMapper, phone)
                .stream().findFirst();
    }

    @Override
    public Optional<AgentAccount> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query(selectBase() + " WHERE email = ?", accountRowMapper, email)
                .stream().findFirst();
    }

    @Override
    public Optional<AgentAccount> findById(Long userId) {
        return jdbcTemplate.query(selectBase() + " WHERE id = ?", accountRowMapper, userId)
                .stream().findFirst();
    }

    @Override
    public List<AgentAccount> findAll(String keyword, String role, String status, int offset, int limit) {
        QueryParts parts = buildFilters(keyword, role, status);
        List<Object> args = new ArrayList<>(parts.args());
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(
                selectBase() + parts.where() + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
                accountRowMapper,
                args.toArray());
    }

    @Override
    public long countAll(String keyword, String role, String status) {
        QueryParts parts = buildFilters(keyword, role, status);
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user" + parts.where(),
                Long.class,
                parts.args().toArray());
        return count == null ? 0 : count;
    }

    @Override
    public long countByRole(String role) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE role_type = ?",
                Long.class,
                toDbRole(role));
        return count == null ? 0 : count;
    }

    @Override
    public long countBeforeMonth() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_user
                WHERE created_at < DATE_SUB(CURDATE(), INTERVAL 1 MONTH)
                """, Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public long countRoleBeforeMonth(String role) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_user
                WHERE role_type = ?
                  AND created_at < DATE_SUB(CURDATE(), INTERVAL 1 MONTH)
                """, Long.class, toDbRole(role));
        return count == null ? 0 : count;
    }

    @Override
    public long countStatusBeforeMonth(String status) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM sys_user
                WHERE status = ?
                  AND created_at < DATE_SUB(CURDATE(), INTERVAL 1 MONTH)
                """, Long.class, status);
        return count == null ? 0 : count;
    }

    @Override
    public AgentAccount updateProfile(Long userId, String name, String email) {
        int rows = jdbcTemplate.update(
                "UPDATE sys_user SET username = ?, email = ? WHERE id = ?",
                name, email, userId);
        assertUpdated(rows);
        return findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    @Override
    public void updateStatus(Long userId, String status) {
        assertUpdated(jdbcTemplate.update(
                "UPDATE sys_user SET status = ? WHERE id = ?", status, userId));
    }

    @Override
    public void updatePassword(Long userId, String passwordHash) {
        assertUpdated(jdbcTemplate.update(
                "UPDATE sys_user SET password_hash = ? WHERE id = ?", passwordHash, userId));
    }

    @Override
    public void saveOperationLog(Long userId, String action, String detail) {
        jdbcTemplate.update("""
                INSERT INTO agent_account_operation_logs (user_id, action, detail)
                VALUES (?, ?, ?)
                """, userId, action, detail);
    }

    @Override
    public List<OperationLog> findOperationLogs(Long userId) {
        return jdbcTemplate.query("""
                SELECT log_id, user_id, action, detail, created_at
                FROM agent_account_operation_logs
                WHERE user_id = ?
                ORDER BY created_at DESC
                """, logRowMapper, userId);
    }

    @Override
    public long countTotalUsers() {
        return countSql("SELECT COUNT(*) FROM sys_user");
    }

    @Override
    public long countCustomerServiceUsers() {
        return countSql("SELECT COUNT(*) FROM sys_user WHERE role_type = 'CUSTOMER_SERVICE'");
    }

    @Override
    public long countAdminUsers() {
        return countSql("SELECT COUNT(*) FROM sys_user WHERE role_type = 'ADMIN'");
    }

    @Override
    public long countDisabledUsers() {
        return countSql("SELECT COUNT(*) FROM sys_user WHERE status = 'DISABLED'");
    }

    private long countSql(String sql) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    private void assertUpdated(int rows) {
        if (rows == 0) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    private String selectBase() {
        return """
                SELECT id AS user_id,
                       username AS name,
                       phone,
                       email,
                       password_hash,
                       role_type AS role,
                       status,
                       created_at,
                       updated_at
                FROM sys_user
                """;
    }

    private QueryParts buildFilters(String keyword, String role, String status) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            clauses.add("(username LIKE ? OR phone LIKE ? OR email LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (role != null && !role.isBlank() && !"ALL".equals(role)) {
            clauses.add("role_type = ?");
            args.add(toDbRole(role));
        }
        if (status != null && !status.isBlank() && !"ALL".equals(status)) {
            clauses.add("status = ?");
            args.add(status);
        }
        return new QueryParts(
                clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses),
                args);
    }

    private static String toDbRole(String role) {
        return "SYSTEM_ADMIN".equals(role) ? "ADMIN" : role;
    }

    private static String fromDbRole(String role) {
        return "ADMIN".equals(role) ? "SYSTEM_ADMIN" : role;
    }

    private record QueryParts(String where, List<Object> args) {
    }
}
