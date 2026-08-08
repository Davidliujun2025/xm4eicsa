package com.carepilot.platform.token;

import com.carepilot.chatworkbench.service.TokenUsageRecordedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tokenmonitor.TokenUsageService;

/** Bridges workbench writes to the token-statistics SSE stream. */
@Component
public class WorkbenchTokenUsageEventListener {
    private final TokenUsageService tokenUsageService;

    public WorkbenchTokenUsageEventListener(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
    }

    @EventListener
    public void onUsageRecorded(TokenUsageRecordedEvent event) {
        tokenUsageService.publishCurrent(event.userId());
    }
}
