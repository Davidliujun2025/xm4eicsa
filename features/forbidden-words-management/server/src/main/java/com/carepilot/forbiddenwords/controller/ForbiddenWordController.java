package com.carepilot.forbiddenwords.controller;

import com.acme.aicslogin.security.AuthenticatedUser;
import com.carepilot.forbiddenwords.dto.CreateWordRequest;
import com.carepilot.forbiddenwords.dto.CsvConfirmRequest;
import com.carepilot.forbiddenwords.dto.CsvPreviewItem;
import com.carepilot.forbiddenwords.dto.ForbiddenWordStats;
import com.carepilot.forbiddenwords.dto.CsvPreviewRequest;
import com.carepilot.forbiddenwords.dto.PageResult;
import com.carepilot.forbiddenwords.model.ForbiddenWord;
import com.carepilot.forbiddenwords.model.OperationAuditLog;
import com.carepilot.forbiddenwords.model.Platform;
import com.carepilot.forbiddenwords.service.ForbiddenWordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forbidden-words")
public class ForbiddenWordController {
    private final ForbiddenWordService forbiddenWordService;

    public ForbiddenWordController(ForbiddenWordService forbiddenWordService) {
        this.forbiddenWordService = forbiddenWordService;
    }

    @GetMapping
    public PageResult<ForbiddenWord> list(
            @RequestParam(required = false) Platform platform,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return forbiddenWordService.list(platform, page, size);
    }

    @GetMapping("/stats")
    public ForbiddenWordStats stats() {
        return forbiddenWordService.stats();
    }

    @PostMapping
    public ForbiddenWord addWord(
            @Valid @RequestBody CreateWordRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest servletRequest
    ) {
        return forbiddenWordService.addWord(
                request.getWord(), request.getPlatform(), user.account(), servletRequest.getRemoteAddr());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteWord(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest servletRequest
    ) {
        forbiddenWordService.removeWord(id, user.account(), servletRequest.getRemoteAddr());

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Deleted");
        result.put("cacheSyncWindowMinutes", forbiddenWordService.getCacheService().getSyncWindowMinutes());
        result.put("lastCacheRefreshAt", forbiddenWordService.getCacheService().getLastRefreshedAt());
        return result;
    }

    @PostMapping("/csv/preview")
    public List<CsvPreviewItem> previewCsv(@Valid @RequestBody CsvPreviewRequest request) {
        return forbiddenWordService.previewCsv(request.getCsvContent());
    }

    @PostMapping("/csv/confirm")
    public Map<String, Object> confirmCsv(
            @Valid @RequestBody CsvConfirmRequest request,
            @AuthenticationPrincipal AuthenticatedUser user,
            HttpServletRequest servletRequest
    ) {
        int importedCount = forbiddenWordService.confirmImport(
                request.getPreviewItems(), user.account(), servletRequest.getRemoteAddr());
        Map<String, Object> result = new HashMap<>();
        result.put("importedCount", importedCount);
        return result;
    }

    @GetMapping("/audit/operations")
    public PageResult<OperationAuditLog> operationLogs(
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return forbiddenWordService.listOperationLogs(operator, action, page, size);
    }
}
