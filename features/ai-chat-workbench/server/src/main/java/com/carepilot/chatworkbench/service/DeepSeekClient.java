package com.carepilot.chatworkbench.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
@Component
public class DeepSeekClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final Integer maxTokens;
    private final boolean thinkingEnabled;

    public DeepSeekClient(RestClient.Builder restClientBuilder,
                          @Value("${app.ai.base-url:https://api.deepseek.com}") String baseUrl,
                          @Value("${app.ai.api-key:}") String apiKey,
                          @Value("${app.ai.model:deepseek-v4-flash}") String model,
                          @Value("${app.ai.max-tokens:1400}") Integer maxTokens,
                          @Value("${app.ai.thinking-enabled:false}") boolean thinkingEnabled) {
        this.restClient = restClientBuilder
                .baseUrl(removeTrailingSlash(baseUrl))
                .build();
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        this.thinkingEnabled = thinkingEnabled;
    }

    public DeepSeekResult recognizeIntent(String renderedPrompt) {
        return complete(
                renderedPrompt,
                "请严格按照提示词中的分析与输出规则完成本次意图识别。");
    }

    public DeepSeekResult complete(String systemPrompt, String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("DeepSeek API Key 未配置，请设置 DEEPSEEK_API_KEY");
        }

        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                        new Message("system", systemPrompt),
                        new Message("user", userPrompt)
                ),
                new Thinking(thinkingEnabled ? "enabled" : "disabled"),
                false,
                maxTokens
        );

        try {
            ChatCompletionResponse response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);
            return toResult(response);
        } catch (RestClientResponseException exception) {
            log.error("DeepSeek request failed with HTTP status {}", exception.getStatusCode());
            throw new DeepSeekApiException(
                    "DeepSeek 接口请求失败（HTTP " + exception.getStatusCode().value() + "）",
                    exception);
        } catch (RestClientException exception) {
            log.error("DeepSeek request failed because the service is unreachable", exception);
            throw new DeepSeekApiException("无法连接 DeepSeek 服务", exception);
        }
    }

    private DeepSeekResult toResult(ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()
                || response.choices().getFirst().message() == null
                || response.choices().getFirst().message().content() == null
                || response.choices().getFirst().message().content().isBlank()) {
            throw new DeepSeekApiException("DeepSeek 返回了空的意图识别结果");
        }

        Usage usage = response.usage();
        int promptTokens = usage == null || usage.promptTokens() == null ? 0 : usage.promptTokens();
        int completionTokens = usage == null || usage.completionTokens() == null ? 0 : usage.completionTokens();
        int totalTokens = usage == null || usage.totalTokens() == null
                ? promptTokens + completionTokens
                : usage.totalTokens();

        return new DeepSeekResult(
                response.choices().getFirst().message().content().trim(),
                promptTokens,
                completionTokens,
                totalTokens,
                response.id(),
                response.model());
    }

    private static String removeTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://api.deepseek.com";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    public record DeepSeekResult(
            String content,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            String requestId,
            String model
    ) {
    }

    private record ChatCompletionRequest(
            String model,
            List<Message> messages,
            Thinking thinking,
            boolean stream,
            @JsonProperty("max_tokens") Integer maxTokens
    ) {
    }

    private record Message(String role, String content) {
    }

    private record Thinking(String type) {
    }

    private record ChatCompletionResponse(
            String id,
            String model,
            List<Choice> choices,
            Usage usage
    ) {
    }

    private record Choice(Message message) {
    }

    private record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {
    }

    public static class DeepSeekApiException extends RuntimeException {
        public DeepSeekApiException(String message) {
            super(message);
        }

        public DeepSeekApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
