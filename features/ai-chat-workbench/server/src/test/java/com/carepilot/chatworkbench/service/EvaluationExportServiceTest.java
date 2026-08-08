package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.entity.CustomerServiceEvaluationReport;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationExportServiceTest {
    private final EvaluationExportService service = new EvaluationExportService();

    @Test
    void exportsRealPdfAndExcelWorkbook() {
        CustomerServiceEvaluationReport report = CustomerServiceEvaluationReport.builder()
                .id(1L).platform("淘宝").totalScore(88).serviceAttitudeScore(90)
                .problemSolvingScore(85).empathyScore(82).complianceScore(95)
                .conversionGuidanceScore(80).responseEfficiencyScore(86)
                .strengths("态度友好").problems("需求确认不足").suggestions("增加澄清问题")
                .forbiddenHitCount(0).forbiddenWordsJson("[]").transcript("客户：你好\n客服：您好")
                .totalTokens(300).generatedAt(LocalDateTime.now()).build();
        byte[] pdf = service.toPdf(report);
        byte[] excel = service.toExcelXml(List.of(report));
        assertThat(new String(pdf, 0, 8, StandardCharsets.ISO_8859_1)).startsWith("%PDF-1.4");
        assertThat(new String(excel, StandardCharsets.UTF_8)).contains("Excel.Sheet", "客服接待评估", "态度友好");
    }
}
