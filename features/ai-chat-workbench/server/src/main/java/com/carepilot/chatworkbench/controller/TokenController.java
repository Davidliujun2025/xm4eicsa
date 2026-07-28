package com.carepilot.chatworkbench.controller;

import com.carepilot.chatworkbench.dto.response.ApiResponse;
import com.carepilot.chatworkbench.dto.response.TokenInfo;
import com.carepilot.chatworkbench.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<TokenInfo>> getTokenUsage(
            @RequestHeader(value = "X-Customer-Id", required = false) String customerIdHeader,
            @RequestParam(value = "customerId", required = false) String customerIdParam,
            @RequestParam(value = "userId", required = false) String userIdParam) {
        String customerId = resolveCustomerId(customerIdHeader, customerIdParam, userIdParam);
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
            @RequestHeader(value = "X-Customer-Id", required = false) String customerIdHeader,
            @RequestParam(value = "customerId", required = false) String customerIdParam,
            @RequestParam(value = "userId", required = false) String userIdParam) {
        String customerId = resolveCustomerId(customerIdHeader, customerIdParam, userIdParam);
        log.info("Check token quota: customerId={}", customerId);
        boolean exceeded = tokenService.isTokenQuotaExceeded(customerId);
        return ResponseEntity.ok(ApiResponse.success(!exceeded));
    }

    private String resolveCustomerId(String customerIdHeader, String customerIdParam, String userIdParam) {
        if (StringUtils.hasText(customerIdHeader)) {
            return customerIdHeader.trim();
        }
        if (StringUtils.hasText(customerIdParam)) {
            return customerIdParam.trim();
        }
        if (StringUtils.hasText(userIdParam)) {
            return userIdParam.trim();
        }
        throw new IllegalArgumentException("缺少客户标识，请传 X-Customer-Id 或 customerId");
    }
}