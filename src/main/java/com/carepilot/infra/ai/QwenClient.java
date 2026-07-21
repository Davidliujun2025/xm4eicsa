package com.carepilot.infra.ai;

/**
 * AI 调用客户端接口。
 * 框架阶段返回占位文本；AI 同学后续替换为真实 DashScope HTTP 调用。
 */
public interface QwenClient {

    /**
     * 调用通义千问生成单步结果。
     *
     * @param prompt 完整的 Prompt 字符串（由 PromptBuilder 组装）
     * @return AI 返回的 JSON 字符串
     * @throws AiGenerationException 调用失败或超时时抛出
     */
    String call(String prompt) throws AiGenerationException;
}
