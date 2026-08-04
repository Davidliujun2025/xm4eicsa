package com.carepilot.chatworkbench.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class DeepSeekLiveIntegrationTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    void recognizesIntentWithTheRealDeepSeekApi() {
        DeepSeekClient client = new DeepSeekClient(
                RestClient.builder(),
                "https://api.deepseek.com",
                System.getenv("DEEPSEEK_API_KEY"),
                "deepseek-v4-flash",
                1400,
                false);
        IntentRecognitionPromptService promptService = new IntentRecognitionPromptService();
        String prompt = promptService.render(
                "京东",
                "物流 配送 时效",
                "请问今天下单明天能送到吗？",
                "无历史上下文");

        DeepSeekClient.DeepSeekResult result = client.recognizeIntent(prompt);

        promptService.validateOutput(result.content());
        assertThat(result.promptTokens()).isPositive();
        assertThat(result.completionTokens()).isPositive();
        assertThat(result.totalTokens())
                .isEqualTo(result.promptTokens() + result.completionTokens());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    void regeneratesAReplyStrategyWithTheRealDeepSeekApi() {
        DeepSeekClient client = new DeepSeekClient(
                RestClient.builder(),
                "https://api.deepseek.com",
                System.getenv("DEEPSEEK_API_KEY"),
                "deepseek-v4-flash",
                1400,
                false);

        DeepSeekClient.DeepSeekResult result = client.complete(
                "你是电商客服回复策略助手。只生成简洁的回复策略正文，不输出Markdown标题。",
                "平台：拼多多；客户问题：这个商品有货吗；意图：客户正在确认库存，尚未表达其他需求。"
                        + "请重新生成回复策略，只输出本步骤正文。"
        );

        assertThat(result.content()).isNotBlank();
        assertThat(result.promptTokens()).isPositive();
        assertThat(result.totalTokens()).isPositive();
    }
}
