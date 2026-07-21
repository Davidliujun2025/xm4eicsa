package com.carepilot.chatworkbench.controller;

import com.carepilot.chatworkbench.dto.response.ApiResponse;
import com.carepilot.chatworkbench.dto.response.TokenInfo;
import com.carepilot.chatworkbench.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<TokenInfo>> getTokenUsage(
            @RequestHeader("X-Customer-Id") String customerId) {
        log.info("Get token usage: customerId={}", customerId);
        Integer usedToday = tokenService.getTodayUsedTokens(customerId);
        Integer dailyLimit = tokenService.getDailyLimit();
        Double usagePercent = tokenService.getUsagePercent(customerId);

        TokenInfo tokenInfo = TokenInfo.builder()
                .currentChatUsage(0)
                .totalLimit(dailyLimit)
                .usedToday(usedToday)
                .usagePercent(usagePercent)
                .build();

        return ResponseEntity.ok(ApiResponse.success(tokenInfo));
    }

    @GetMapping("/quota-check")
    public ResponseEntity<ApiResponse<Boolean>> checkQuota(
            @RequestHeader("X-Customer-Id") String customerId) {
        log.info("Check token quota: customerId={}", customerId);
        boolean exceeded = tokenService.isTokenQuotaExceeded(customerId);
        return ResponseEntity.ok(ApiResponse.success(!exceeded));
    }
}