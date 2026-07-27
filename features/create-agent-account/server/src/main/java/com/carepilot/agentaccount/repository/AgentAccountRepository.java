package com.carepilot.agentaccount.repository;


import java.util.List;
import java.util.Optional;

import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.model.OperationLog;


public interface AgentAccountRepository {


    AgentAccount save(AgentAccount account);


    Optional<AgentAccount> findByPhone(String phone);


    Optional<AgentAccount> findByEmail(String email);


    Optional<AgentAccount> findById(Long userId);

    long countByRole(String role);
    long countBeforeMonth();
    long countRoleBeforeMonth(String role);
    long countStatusBeforeMonth(String status);

    List<AgentAccount> findAll(
            String keyword,
            String role,
            String status,
            int offset,
            int limit
    );


    long countAll(
            String keyword,
            String role,
            String status
    );



    AgentAccount updateProfile(
            Long userId,
            String name,
            String email
    );


    void updateStatus(
            Long userId,
            String status
    );


    void updatePassword(
            Long userId,
            String passwordHash
    );


    void saveOperationLog(
            Long userId,
            String action,
            String detail
    );


    List<OperationLog> findOperationLogs(
            Long userId
    );


    // ==========================
    // 用户统计接口
    // ==========================

    long countTotalUsers();


    long countCustomerServiceUsers();


    long countAdminUsers();


    long countDisabledUsers();

}