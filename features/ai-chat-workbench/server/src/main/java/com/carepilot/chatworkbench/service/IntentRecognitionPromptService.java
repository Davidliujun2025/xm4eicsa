package com.carepilot.chatworkbench.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class IntentRecognitionPromptService {

    private static final List<String> REQUIRED_HEADINGS = List.of(
            "消费者情绪/心情：",
            "进店理由/场景：",
            "痛点/爽点：",
            "购买意向：",
            "性格/决策链路：",
            "结论："
    );

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
        if (!normalized.startsWith(REQUIRED_HEADINGS.getFirst())) {
            throw new DeepSeekClient.DeepSeekApiException("意图识别结果包含固定格式之外的前置内容");
        }
        int previousIndex = -1;
        for (String heading : REQUIRED_HEADINGS) {
            int headingIndex = normalized.indexOf(heading);
            if (headingIndex < 0) {
                throw new DeepSeekClient.DeepSeekApiException(
                        "意图识别结果格式不完整，缺少固定标题：" + heading);
            }
            if (headingIndex <= previousIndex) {
                throw new DeepSeekClient.DeepSeekApiException("意图识别结果的固定标题顺序错误");
            }
            previousIndex = headingIndex;
        }
        if (normalized.contains("```") || normalized.contains("**")) {
            throw new DeepSeekClient.DeepSeekApiException("意图识别结果包含不允许的 Markdown 格式");
        }
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
