package com.carepilot.chatworkbench.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
@Component
public class IntentRecognitionPromptService {

    private final String template;

    public IntentRecognitionPromptService() {
        try {
            template = new ClassPathResource("prompts/intent-recognition.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("无法加载意图识别提示词", exception);
        }
    }

    public String render(String platform, String keywords, String customerDialog,
                         String conversationContext) {
        return template
                .replace("{{platform}}", safeValue(platform, "未指定"))
                .replace("{{keywords}}", safeValue(keywords, "未提取到明确关键词"))
                .replace("{{customer_dialog}}", safeValue(customerDialog, "未提供"))
                .replace("{{conversation_context}}", safeValue(conversationContext, "无历史上下文"));
    }

    public void validateOutput(String content) {
        if (content == null || content.isBlank()) {
            throw new DeepSeekClient.DeepSeekApiException("意图识别结果为空");
        }
        String normalized = content.trim();
        if (normalized.startsWith("{") || normalized.startsWith("[")
                || normalized.contains("```") || normalized.contains("**")
                || normalized.lines().anyMatch(line -> line.trim().startsWith("|"))) {
            throw new DeepSeekClient.DeepSeekApiException("意图识别结果包含不允许的 JSON、表格或代码格式");
        }
        if (!normalized.contains("结论：") && !normalized.contains("结论:")) {
            throw new DeepSeekClient.DeepSeekApiException("意图识别结果缺少必需的结论");
        }
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
