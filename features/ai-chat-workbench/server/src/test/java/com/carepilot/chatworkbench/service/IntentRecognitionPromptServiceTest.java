package com.carepilot.chatworkbench.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntentRecognitionPromptServiceTest {

    private final IntentRecognitionPromptService service = new IntentRecognitionPromptService();

    @Test
    void rendersAllRuntimeInputsIntoTxtTemplate() {
        String prompt = service.render(
                "京东", "物流 配送", "今天能送到吗？", "第1轮：什么时候发货？");

        assertThat(prompt)
                .contains("服务平台：京东")
                .contains("关键词：物流 配送")
                .contains("当前客户消息：今天能送到吗？")
                .contains("当前对话历史上下文：第1轮：什么时候发货？")
                .doesNotContain("{{platform}}", "{{keywords}}", "{{customer_dialog}}",
                        "{{conversation_context}}");
    }

    @Test
    void validatesTheSixRequiredSections() {
        String output = """
                消费者情绪/心情：平和

                进店理由/场景：咨询物流

                痛点/爽点：在意时效

                购买意向：尚无法确认

                性格/决策链路：重视效率

                结论：信息充分
                """;

        service.validateOutput(output);
    }

    @Test
    void rejectsAnIncompleteOutput() {
        assertThatThrownBy(() -> service.validateOutput("消费者情绪/心情：平和"))
                .isInstanceOf(DeepSeekClient.DeepSeekApiException.class)
                .hasMessageContaining("进店理由/场景");
    }

    @Test
    void rejectsExtraTextBeforeTheFixedSections() {
        assertThatThrownBy(() -> service.validateOutput("以下是分析结果：\n消费者情绪/心情：平和"))
                .isInstanceOf(DeepSeekClient.DeepSeekApiException.class)
                .hasMessageContaining("前置内容");
    }
}
