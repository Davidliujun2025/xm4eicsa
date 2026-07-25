package com.carepilot.agentaccount.repository;

import com.carepilot.agentaccount.model.AgentAccount;

import java.util.Optional;

public interface AgentAccountRepository {

    AgentAccount save(AgentAccount account);

    Optional<AgentAccount> findByPhone(String phone);

    Optional<AgentAccount> findByEmail(String email);

    Optional<AgentAccount> findById(Long userId);
}