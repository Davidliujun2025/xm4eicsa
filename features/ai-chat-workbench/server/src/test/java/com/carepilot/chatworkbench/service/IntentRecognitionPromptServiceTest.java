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
                .contains("只输出有明确对话证据的分析维度")
                .contains("无法识别、证据不足或仅有无依据猜测的维度必须省略")
                .contains("结论”必须输出")
                .contains("每个已输出维度结束后换行并空一行")
                .contains("消费者情绪/心情：……\n\n进店理由/场景：……")
                .contains("全部正文控制在100至200个汉字且不得超过200字")
                .contains("引导问题”标题和具体问题外")
                .contains("少于3个时，在结论后追加1至3条")
                .contains("引导问题：\n1.")
                .contains("语气平和、带有试探性")
                .contains("实际内容必须随输入变化，不得照抄示例判断")
                .doesNotContain("{{platform}}", "{{keywords}}", "{{customer_dialog}}",
                        "{{conversation_context}}");
    }

    @Test
    void acceptsEvidenceBasedOutputWithOmittedUnknownDimensions() {
        String output = """
                场景：客户提到“已经下单”，当前处于物流查询阶段。
                痛点/爽点：客户原话询问“今天能送到吗”，明确关注配送时效。
                性格：当前决策表现可能偏效率优先，依据是主动确认到货时间。
                结论：客户希望确认已购商品能否在今天送达，当前诉求较明确。
                """;

        service.validateOutput(output);
    }

    @Test
    void rejectsOutputWithoutRequiredConclusion() {
        assertThatThrownBy(() -> service.validateOutput("场景：客户正在查询物流。"))
                .isInstanceOf(DeepSeekClient.DeepSeekApiException.class)
                .hasMessageContaining("结论");
    }

    @Test
    void rejectsJsonTableOrCodeOutput() {
        assertThatThrownBy(() -> service.validateOutput("{\"结论\":\"物流查询\"}"))
                .isInstanceOf(DeepSeekClient.DeepSeekApiException.class)
                .hasMessageContaining("JSON");
    }
}
