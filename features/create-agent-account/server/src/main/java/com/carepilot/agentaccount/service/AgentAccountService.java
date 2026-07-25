package com.carepilot.agentaccount.service;

import com.carepilot.agentaccount.dto.CreateAgentAccountRequest;
import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.repository.AgentAccountRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AgentAccountService {

    private static final String DEFAULT_ROLE = "CUSTOMER_SERVICE";
    private static final String DEFAULT_STATUS = "ENABLED";

    private final AgentAccountRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AgentAccountService(AgentAccountRepository repository) {
        this.repository = repository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public AgentAccount createAccount(CreateAgentAccountRequest request) {
        String name = request.getName().trim();
        String phone = request.getPhone().trim();
        String email = normalizeEmail(request.getEmail());
        String initialPassword = request.getInitialPassword();

        if (repository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("该手机号已存在");
        }

        if (email != null && repository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("该邮箱已存在");
        }

        if (initialPassword.equals(phone)) {
            throw new IllegalArgumentException("初始密码不能与手机号完全一致");
        }

        AgentAccount account = new AgentAccount();
        account.setName(name);
        account.setPhone(phone);
        account.setEmail(email);
        account.setPasswordHash(passwordEncoder.encode(initialPassword));
        account.setRole(DEFAULT_ROLE);
        account.setStatus(DEFAULT_STATUS);

        return repository.save(account);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase();
    }
}