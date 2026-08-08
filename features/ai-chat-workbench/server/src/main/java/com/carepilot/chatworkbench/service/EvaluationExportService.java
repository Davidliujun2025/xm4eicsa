package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.entity.CustomerServiceEvaluationReport;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvaluationExportService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] toExcelXml(List<CustomerServiceEvaluationReport> reports) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <?mso-application progid="Excel.Sheet"?>
                <Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
                  xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
                <Worksheet ss:Name="客服接待评估"><Table>
                """);
        row(xml, "报告ID", "评估时间", "平台", "总分", "服务态度", "问题解决", "共情能力",
                "合规性", "转化引导", "响应效率", "违禁词命中", "Token消耗", "做得好的地方",
                "存在的问题", "改进建议");
        for (CustomerServiceEvaluationReport report : reports) {
            row(xml, String.valueOf(report.getId()),
                    report.getGeneratedAt() == null ? "" : DATE_TIME.format(report.getGeneratedAt()),
                    report.getPlatform(), String.valueOf(report.getTotalScore()),
                    String.valueOf(report.getServiceAttitudeScore()), String.valueOf(report.getProblemSolvingScore()),
                    String.valueOf(report.getEmpathyScore()), String.valueOf(report.getComplianceScore()),
                    String.valueOf(report.getConversionGuidanceScore()), String.valueOf(report.getResponseEfficiencyScore()),
                    String.valueOf(report.getForbiddenHitCount()), String.valueOf(report.getTotalTokens()),
                    report.getStrengths(), report.getProblems(), report.getSuggestions());
        }
        xml.append("</Table></Worksheet></Workbook>");
        return xml.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] toPdf(CustomerServiceEvaluationReport report) {
        List<String> lines = new ArrayList<>();
        addWrapped(lines, "客服接待评估报告", 34);
        addWrapped(lines, "报告编号：" + report.getId() + "　平台：" + safe(report.getPlatform()), 34);
        addWrapped(lines, "生成时间：" + (report.getGeneratedAt() == null ? "" : DATE_TIME.format(report.getGeneratedAt())), 34);
        addWrapped(lines, "总体评分：" + report.getTotalScore() + " / 100", 34);
        addWrapped(lines, "服务态度：" + report.getServiceAttitudeScore() + "　问题解决：" + report.getProblemSolvingScore()
                + "　共情能力：" + report.getEmpathyScore(), 34);
        addWrapped(lines, "合规性：" + report.getComplianceScore() + "　转化引导：" + report.getConversionGuidanceScore()
                + "　响应效率：" + report.getResponseEfficiencyScore(), 34);
        addWrapped(lines, "做得好的地方：" + safe(report.getStrengths()), 34);
        addWrapped(lines, "存在的问题：" + safe(report.getProblems()), 34);
        addWrapped(lines, "改进建议：" + safe(report.getSuggestions()), 34);
        addWrapped(lines, "违禁词命中：" + report.getForbiddenHitCount() + " 次　命中词：" + safe(report.getForbiddenWordsJson()), 34);
        addWrapped(lines, "本次 Token 消耗：" + report.getTotalTokens(), 34);
        lines.add("原始对话记录：");
        for (String sourceLine : safe(report.getTranscript()).split("\\R")) addWrapped(lines, sourceLine, 34);
        return buildPdf(lines);
    }

    private void row(StringBuilder xml, String... values) {
        xml.append("<Row>");
        for (String value : values) xml.append("<Cell><Data ss:Type=\"String\">").append(xml(value)).append("</Data></Cell>");
        xml.append("</Row>");
    }

    private String xml(String value) {
        return safe(value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private void addWrapped(List<String> target, String value, int width) {
        int[] points = safe(value).codePoints().toArray();
        if (points.length == 0) { target.add(""); return; }
        for (int from = 0; from < points.length; from += width) {
            target.add(new String(points, from, Math.min(width, points.length - from)));
        }
    }

    private String safe(String value) { return value == null ? "" : value; }

    private byte[] buildPdf(List<String> lines) {
        int linesPerPage = 46;
        int pageCount = Math.max(1, (lines.size() + linesPerPage - 1) / linesPerPage);
        int fontObject = 3 + pageCount * 2;
        int descendantFontObject = fontObject + 1;
        List<byte[]> objects = new ArrayList<>();
        objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder();
        for (int page = 0; page < pageCount; page++) kids.append(3 + page * 2).append(" 0 R ");
        objects.add(bytes("<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>"));
        for (int page = 0; page < pageCount; page++) {
            int pageObject = 3 + page * 2;
            int contentObject = pageObject + 1;
            objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 "
                    + fontObject + " 0 R >> >> /Contents " + contentObject + " 0 R >>"));
            StringBuilder content = new StringBuilder("BT /F1 10 Tf 45 805 Td 16 TL\n");
            int start = page * linesPerPage;
            int end = Math.min(lines.size(), start + linesPerPage);
            for (int i = start; i < end; i++) content.append('<').append(utf16Hex(lines.get(i))).append("> Tj T*\n");
            content.append("ET");
            byte[] stream = bytes(content.toString());
            objects.add(bytes("<< /Length " + stream.length + " >>\nstream\n" + content + "\nendstream"));
        }
        objects.add(bytes("<< /Type /Font /Subtype /Type0 /BaseFont /STSong-Light /Encoding /UniGB-UCS2-H "
                + "/DescendantFonts [" + descendantFontObject + " 0 R] >>"));
        objects.add(bytes("<< /Type /Font /Subtype /CIDFontType0 /BaseFont /STSong-Light "
                + "/CIDSystemInfo << /Registry (Adobe) /Ordering (GB1) /Supplement 4 >> >>"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        write(output, "%PDF-1.4\n%PDF-CJK\n");
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(output.size());
            write(output, (i + 1) + " 0 obj\n");
            output.writeBytes(objects.get(i));
            write(output, "\nendobj\n");
        }
        int xref = output.size();
        write(output, "xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n");
        for (int i = 1; i < offsets.size(); i++) write(output, "%010d 00000 n \n".formatted(offsets.get(i)));
        write(output, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF");
        return output.toByteArray();
    }

    private String utf16Hex(String value) {
        byte[] data = safe(value).getBytes(StandardCharsets.UTF_16BE);
        StringBuilder hex = new StringBuilder(data.length * 2);
        for (byte item : data) hex.append("%02X".formatted(item & 0xff));
        return hex.toString();
    }

    private byte[] bytes(String value) { return value.getBytes(StandardCharsets.ISO_8859_1); }
    private void write(ByteArrayOutputStream output, String value) { output.writeBytes(bytes(value)); }
}
