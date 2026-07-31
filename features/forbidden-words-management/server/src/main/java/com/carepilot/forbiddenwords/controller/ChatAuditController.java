package com.carepilot.forbiddenwords.controller;

import com.carepilot.forbiddenwords.dto.ChatAuditRequest;
import com.carepilot.forbiddenwords.dto.PageResult;
import com.carepilot.forbiddenwords.model.HitAuditLog;
import com.carepilot.forbiddenwords.model.Platform;
import com.carepilot.forbiddenwords.service.HitAuditService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat-audit")
public class ChatAuditController {
    private final HitAuditService hitAuditService;

    public ChatAuditController(HitAuditService hitAuditService) {
        this.hitAuditService = hitAuditService;
    }

    @PostMapping("/check")
    public Map<String, Object> check(@Valid @RequestBody ChatAuditRequest request) {
        return hitAuditService.auditChat(request);
    }

    @GetMapping("/logs")
    public PageResult<HitAuditLog> logs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) Platform platform,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return hitAuditService.listHitLogs(actor, platform, page, size);
    }
}
