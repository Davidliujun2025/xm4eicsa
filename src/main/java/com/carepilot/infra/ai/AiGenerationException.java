package com.carepilot.infra.ai;

/**
 * AI 生成异常，在 Qwen API 调用失败或超时时抛出。
 */
public class AiGenerationException extends RuntimeException {

    public AiGenerationException(String message) {
        super(message);
    }

    public AiGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
