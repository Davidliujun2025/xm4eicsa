package com.carepilot.agentaccount.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.model.OperationLog;

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
        account.setRole(rs.getString("role"));
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

    public JdbcAgentAccountRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override
    public AgentAccount save(AgentAccount account) {
        jdbcTemplate.update("""
                INSERT INTO agent_accounts (name, phone, email, password_hash, role, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """, account.getName(), account.getPhone(), account.getEmail(), account.getPasswordHash(), account.getRole(), account.getStatus());
        return findByPhone(account.getPhone()).orElseThrow(() -> new IllegalStateException("账号创建后无法读取"));
    }

    @Override
    public Optional<AgentAccount> findByPhone(String phone) {
        return jdbcTemplate.query(selectBase() + " WHERE phone = ?", accountRowMapper, phone).stream().findFirst();
    }

    @Override
    public Optional<AgentAccount> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return jdbcTemplate.query(selectBase() + " WHERE email = ?", accountRowMapper, email).stream().findFirst();
    }

    @Override
    public Optional<AgentAccount> findById(Long userId) {
        return jdbcTemplate.query(selectBase() + " WHERE user_id = ?", accountRowMapper, userId).stream().findFirst();
    }

    @Override
    public List<AgentAccount> findAll(String keyword, String role, String status, int offset, int limit) {
        QueryParts parts = buildFilters(keyword, role, status);
        List<Object> args = new ArrayList<>(parts.args());
        args.add(limit);
        args.add(offset);
        return jdbcTemplate.query(selectBase() + parts.where() + " ORDER BY created_at DESC LIMIT ? OFFSET ?", accountRowMapper, args.toArray());
    }

    @Override
    public long countAll(String keyword, String role, String status) {
        QueryParts parts = buildFilters(keyword, role, status);
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM agent_accounts" + parts.where(), Long.class, parts.args().toArray());
        return count == null ? 0 : count;
    }

    @Override
    public long countByRole(String role) {
    
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM agent_accounts WHERE role = ?",
            Integer.class,
            role
        );
    
        return count == null ? 0 : count;
    }

    @Override
    public long countBeforeMonth(){
    
        String sql =
            """
            SELECT COUNT(*)
            FROM agent_accounts
            WHERE created_at < DATE_SUB(CURDATE(), INTERVAL 1 MONTH)
            """;
    
        return jdbcTemplate.queryForObject(
            sql,
            Long.class
        );
    }    

    @Override
    public long countRoleBeforeMonth(String role){
    
        String sql =
            """
            SELECT COUNT(*)
            FROM agent_accounts
            WHERE role = ?
            AND created_at < DATE_SUB(CURDATE(), INTERVAL 1 MONTH)
            """;
    
        return jdbcTemplate.queryForObject(
            sql,
            Long.class,
            role
        );
    }    

    @Override
    public long countStatusBeforeMonth(String status){
    
        String sql =
            """
            SELECT COUNT(*)
            FROM agent_accounts
            WHERE status = ?
            AND created_at < DATE_SUB(CURDATE(), INTERVAL 1 MONTH)
            """;
    
        return jdbcTemplate.queryForObject(
            sql,
            Long.class,
            status
        );
    }
        
    @Override
    public AgentAccount updateProfile(Long userId, String name, String email) {
        int rows = jdbcTemplate.update("UPDATE agent_accounts SET name = ?, email = ? WHERE user_id = ?", name, email, userId);
        if (rows == 0) throw new IllegalArgumentException("用户不存在");
        return findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    @Override
    public void updateStatus(Long userId,String status) {
        int rows =jdbcTemplate.update("UPDATE agent_accounts SET status=? WHERE user_id=?",status,userId);
        if(rows==0){throw new IllegalArgumentException("用户不存在");}
    }

    @Override
    public void updatePassword(Long userId, String passwordHash) {
        int rows = jdbcTemplate.update("UPDATE agent_accounts SET password_hash = ? WHERE user_id = ?", passwordHash, userId);
        if (rows == 0) throw new IllegalArgumentException("用户不存在");
    }

    @Override
    public void saveOperationLog(Long userId, String action, String detail) {
        jdbcTemplate.update("INSERT INTO agent_account_operation_logs (user_id, action, detail) VALUES (?, ?, ?)", userId, action, detail);
    }

    @Override
    public List<OperationLog> findOperationLogs(Long userId) {
        return jdbcTemplate.query("""
                SELECT log_id, user_id, action, detail, created_at
                FROM agent_account_operation_logs
                WHERE user_id = ? ORDER BY created_at DESC
                """, logRowMapper, userId);
    }
    @Override
    public long countTotalUsers(){
    
        String sql =
                """
                select count(*)
                from agent_account
                """;
    
    
        return jdbcTemplate.queryForObject(
                sql,
                Long.class
        );
    }
    
    
    
    @Override
    public long countCustomerServiceUsers(){
    
        String sql =
                """
                select count(*)
                from agent_account
                where role='CUSTOMER_SERVICE'
                """;
    
    
        return jdbcTemplate.queryForObject(
                sql,
                Long.class
        );
    }
    
    
    
    @Override
    public long countAdminUsers(){
    
        String sql =
                """
                select count(*)
                from agent_account
                where role='SYSTEM_ADMIN'
                """;
    
    
        return jdbcTemplate.queryForObject(
                sql,
                Long.class
        );
    }
    
    
    
    @Override
    public long countDisabledUsers(){
    
        String sql =
                """
                select count(*)
                from agent_account
                where status='DISABLED'
                """;
    
    
        return jdbcTemplate.queryForObject(
                sql,
                Long.class
        );
    }    

    private String selectBase() {
        return "SELECT user_id, name, phone, email, password_hash, role, status, created_at, updated_at FROM agent_accounts";
    }

    private QueryParts buildFilters(String keyword, String role, String status) {
        List<String> clauses = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            clauses.add("(name LIKE ? OR phone LIKE ? OR email LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like); args.add(like); args.add(like);
        }
        if (role != null && !role.isBlank() && !"ALL".equals(role)) { clauses.add("role = ?"); args.add(role); }
        if (status != null && !status.isBlank() && !"ALL".equals(status)) { clauses.add("status = ?"); args.add(status); }
        return new QueryParts(clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses), args);
    }

    private record QueryParts(String where, List<Object> args) {}
}
