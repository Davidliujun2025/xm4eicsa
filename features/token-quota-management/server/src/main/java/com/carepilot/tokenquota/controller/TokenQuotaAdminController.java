package com.carepilot.tokenquota.controller;

import com.carepilot.tokenquota.common.ApiResponse;
import com.carepilot.tokenquota.dto.AdjustmentLogResponse;
import com.carepilot.tokenquota.dto.BatchUpdateDailyQuotaRequest;
import com.carepilot.tokenquota.dto.BatchUpdateDailyQuotaResponse;
import com.carepilot.tokenquota.dto.PageResponse;
import com.carepilot.tokenquota.dto.RecordTokenUsageRequest;
import com.carepilot.tokenquota.dto.RecordTokenUsageResponse;
import com.carepilot.tokenquota.dto.TokenQuotaItemResponse;
import com.carepilot.tokenquota.dto.TokenQuotaSummaryResponse;
import com.carepilot.tokenquota.dto.UpdateDailyQuotaRequest;
import com.carepilot.tokenquota.dto.UpdateDailyQuotaResponse;
import com.carepilot.tokenquota.service.TokenQuotaService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/admin/token-quotas", "/api/token-quota"})
public class TokenQuotaAdminController {
    private final TokenQuotaService tokenQuotaService;

    public TokenQuotaAdminController(TokenQuotaService tokenQuotaService) {
        this.tokenQuotaService = tokenQuotaService;
    }

    @GetMapping("/accounts")
    public ApiResponse<PageResponse<TokenQuotaItemResponse>> listAccounts(
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(tokenQuotaService.listAccounts(accountNo, statusCode, page, pageSize));
    }

    @GetMapping("/accounts/{accountNo}")
    public ApiResponse<TokenQuotaItemResponse> getAccount(
            @PathVariable String accountNo
    ) {
        return ApiResponse.ok(tokenQuotaService.getAccount(accountNo));
    }

    @GetMapping("/summary")
    public ApiResponse<TokenQuotaSummaryResponse> getSummary() {
        return ApiResponse.ok(tokenQuotaService.getSummary());
    }

    @PutMapping("/accounts/{accountNo}/daily-quota")
    public ApiResponse<UpdateDailyQuotaResponse> updateDailyQuota(
            @PathVariable String accountNo,
            @Valid @RequestBody UpdateDailyQuotaRequest request
    ) {
        return ApiResponse.ok("额度设置成功", tokenQuotaService.updateDailyQuota(accountNo, request));
    }

    @PutMapping("/accounts/daily-quota/batch")
    public ApiResponse<BatchUpdateDailyQuotaResponse> batchUpdateDailyQuota(
            @Valid @RequestBody BatchUpdateDailyQuotaRequest request
    ) {
        return ApiResponse.ok("额度设置成功", tokenQuotaService.batchUpdateDailyQuota(request));
    }

    @PostMapping("/usage-records")
    public ApiResponse<RecordTokenUsageResponse> recordUsage(
            @Valid @RequestBody RecordTokenUsageRequest request
    ) {
        return ApiResponse.ok(tokenQuotaService.recordUsage(request));
    }

    @GetMapping("/adjustment-logs")
    public ApiResponse<PageResponse<AdjustmentLogResponse>> listAdjustmentLogs(
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endTime,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize
    ) {
        return ApiResponse.ok(tokenQuotaService.listAdjustmentLogs(accountNo, startTime, endTime, page, pageSize));
    }
}
