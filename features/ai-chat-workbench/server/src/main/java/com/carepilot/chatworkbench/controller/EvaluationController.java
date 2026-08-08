package com.carepilot.chatworkbench.controller;

import com.acme.aicslogin.security.AuthenticatedUser;
import com.carepilot.chatworkbench.dto.response.EvaluationPageResponse;
import com.carepilot.chatworkbench.dto.response.EvaluationStatsResponse;
import com.carepilot.chatworkbench.entity.CustomerServiceEvaluationReport;
import com.carepilot.chatworkbench.service.EvaluationExportService;
import com.carepilot.chatworkbench.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
public class EvaluationController {
    private final EvaluationService evaluationService;
    private final EvaluationExportService exportService;

    @GetMapping("/me")
    public EvaluationPageResponse myReports(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer maxScore,
            @RequestParam(required = false) String platform,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return evaluationService.searchMine(user.id().toString(), from, to, minScore, maxScore, platform, page, size);
    }

    @GetMapping("/me/stats")
    public EvaluationStatsResponse myStats(@AuthenticationPrincipal AuthenticatedUser user) {
        return evaluationService.statsMine(user.id().toString());
    }

    @GetMapping("/me/{reportId}")
    public CustomerServiceEvaluationReport myReport(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long reportId) {
        return evaluationService.getMine(user.id().toString(), reportId);
    }

    @GetMapping("/me/{reportId}/export.pdf")
    public ResponseEntity<byte[]> exportPdf(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long reportId) {
        CustomerServiceEvaluationReport report = evaluationService.getMine(user.id().toString(), reportId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=evaluation-" + reportId + ".pdf")
                .body(exportService.toPdf(report));
    }

    @GetMapping("/me/export.xls")
    public ResponseEntity<byte[]> exportExcel(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer maxScore,
            @RequestParam(required = false) String platform) {
        List<CustomerServiceEvaluationReport> reports = evaluationService.allMine(
                user.id().toString(), from, to, minScore, maxScore, platform);
        String encodedName = java.net.URLEncoder.encode("客服接待评估汇总.xls", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.ms-excel"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(exportService.toExcelXml(reports));
    }
}
