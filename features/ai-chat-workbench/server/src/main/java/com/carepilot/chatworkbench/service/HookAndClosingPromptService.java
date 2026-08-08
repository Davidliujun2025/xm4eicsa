package com.carepilot.chatworkbench.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Prompt contracts and output validation for steps four and five. */
@Service
public class HookAndClosingPromptService {

    private final String hookTemplate;
    private final String closingTemplate;

    public HookAndClosingPromptService() {
        hookTemplate = load("prompts/hook-guidance.txt", "钩子引导");
        closingTemplate = load("prompts/successful-closing.txt", "成功收尾");
    }

    public String render(int stepNo) {
        return switch (stepNo) {
            case 4 -> hookTemplate;
            case 5 -> closingTemplate;
            default -> throw new IllegalArgumentException("仅支持第四步和第五步提示词");
        };
    }

    public void validateOutput(int stepNo, String content) {
        validatePlainText(content);
        String normalized = content.trim();
        if (stepNo == 4) {
            require(normalized, "钩子话术内容", "第四步缺少钩子话术内容");
            require(normalized, "理由说明", "第四步缺少理由说明");
            require(normalized, "预期效果", "第四步缺少预期效果");
            return;
        }
        if (stepNo == 5) {
            boolean hasClosing = normalized.contains("成功收尾话术")
                    || normalized.contains("预生成收尾话术");
            if (!hasClosing) {
                throw new DeepSeekClient.DeepSeekApiException("第五步缺少成功收尾或预生成收尾话术");
            }
            return;
        }
        throw new IllegalArgumentException("仅支持第四步和第五步输出校验");
    }

    private String load(String path, String name) {
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("无法加载" + name + "提示词", exception);
        }
    }

    private void validatePlainText(String content) {
        if (content == null || content.isBlank()) {
            throw new DeepSeekClient.DeepSeekApiException("步骤生成结果为空");
        }
        String normalized = content.trim();
        if (normalized.startsWith("{") || normalized.startsWith("[")
                || normalized.contains("```")
                || normalized.lines().anyMatch(line -> line.trim().startsWith("|"))) {
            throw new DeepSeekClient.DeepSeekApiException("步骤结果包含不允许的 JSON、表格或代码格式");
        }
    }

    private void require(String content, String marker, String message) {
        if (!content.contains(marker)) {
            throw new DeepSeekClient.DeepSeekApiException(message);
        }
    }
}
