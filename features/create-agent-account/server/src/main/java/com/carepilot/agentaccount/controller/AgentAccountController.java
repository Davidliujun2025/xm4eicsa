package com.carepilot.agentaccount.controller;

import com.carepilot.agentaccount.dto.CreateAgentAccountRequest;
import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.service.AgentAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/customer-service-users")
public class AgentAccountController {

    private final AgentAccountService agentAccountService;

    public AgentAccountController(AgentAccountService agentAccountService) {
        this.agentAccountService = agentAccountService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAccount(
            @Valid @RequestBody CreateAgentAccountRequest request) {

        AgentAccount account = agentAccountService.createAccount(request);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", account.getUserId());
        data.put("name", account.getName());
        data.put("phone", account.getPhone());
        data.put("email", account.getEmail());
        data.put("role", account.getRole());
        data.put("roleName", "客服人员");
        data.put("status", account.getStatus());
        data.put("statusName", "启用");
        data.put("createdAt", account.getCreatedAt());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", "SUCCESS");
        response.put("message", "客服人员账号创建成功");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}