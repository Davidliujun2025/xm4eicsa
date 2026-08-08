package com.carepilot.chatworkbench.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Loads and validates the prompt contract for step three: recommended script. */
@Service
public class RecommendedScriptPromptService {

    private final String template;

    public RecommendedScriptPromptService() {
        try {
            template = new ClassPathResource("prompts/recommended-script.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("无法加载推荐话术提示词", exception);
        }
    }

    public String render(String platform) {
        String normalizedPlatform = platform == null || platform.isBlank()
                ? "通用电商"
                : platform.trim();
        return template.replace("{{platform}}", normalizedPlatform);
    }

    public void validateOutput(String content) {
        if (content == null || content.isBlank()) {
            throw new DeepSeekClient.DeepSeekApiException("推荐话术结果为空");
        }
        String normalized = content.trim();
        if (normalized.startsWith("{") || normalized.startsWith("[")
                || normalized.contains("```")
                || normalized.lines().anyMatch(line -> line.trim().startsWith("|"))) {
            throw new DeepSeekClient.DeepSeekApiException("推荐话术包含不允许的 JSON、表格或代码格式");
        }
        if ((!normalized.contains("直接复制话术（") && !normalized.contains("直接复制话术("))
                || (!normalized.contains("话术点评：") && !normalized.contains("话术点评:"))) {
            throw new DeepSeekClient.DeepSeekApiException("推荐话术缺少“直接复制话术”或“话术点评”部分");
        }
    }
}
