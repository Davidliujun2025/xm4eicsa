package com.carepilot.chatworkbench.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Loads the prompt contract used by step two: reply strategy. */
@Service
public class ReplyStrategyPromptService {

    private final String template;

    public ReplyStrategyPromptService() {
        try {
            template = new ClassPathResource("prompts/reply-strategy.txt")
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("无法加载回复策略提示词", exception);
        }
    }

    public String render() {
        return template;
    }

    public void validateOutput(String content) {
        if (content == null || content.isBlank()) {
            throw new DeepSeekClient.DeepSeekApiException("回复策略结果为空");
        }
        String normalized = content.trim();
        if (normalized.startsWith("{") || normalized.startsWith("[")
                || normalized.contains("```")
                || normalized.lines().anyMatch(line -> line.trim().startsWith("|"))) {
            throw new DeepSeekClient.DeepSeekApiException("回复策略包含不允许的 JSON、表格或代码格式");
        }
    }
}
