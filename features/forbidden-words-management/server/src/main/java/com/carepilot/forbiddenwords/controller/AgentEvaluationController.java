package com.carepilot.forbiddenwords.controller;

import com.acme.aicslogin.security.AuthenticatedUser;
import com.carepilot.forbiddenwords.dto.PageResult;
import com.carepilot.forbiddenwords.model.HitAuditLog;
import com.carepilot.forbiddenwords.service.HitAuditService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/evaluations")
public class AgentEvaluationController {

    private final HitAuditService hitAuditService;

    public AgentEvaluationController(HitAuditService hitAuditService) {
        this.hitAuditService = hitAuditService;
    }

    @GetMapping("/me")
    public PageResult<HitAuditLog> myEvaluations(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Set<String> actorKeys = new HashSet<>();
        actorKeys.add(user.id().toString());
        actorKeys.add(user.account());
        actorKeys.add(user.displayName());
        return hitAuditService.listCurrentAgentHitLogs(actorKeys, page, size);
    }
}
