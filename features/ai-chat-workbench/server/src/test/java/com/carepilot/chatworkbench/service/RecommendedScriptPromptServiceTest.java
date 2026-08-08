package com.carepilot.chatworkbench.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecommendedScriptPromptServiceTest {

    private final RecommendedScriptPromptService service = new RecommendedScriptPromptService();

    @Test
    void rendersPlatformAndRequiredOutputContract() {
        String prompt = service.render("淘宝");

        assertThat(prompt)
                .contains("第三步：推荐话术")
                .contains("直接复制话术（淘宝版）：")
                .contains("话术点评：")
                .contains("客户当前问题", "意图识别", "回复策略", "历史上下文")
                .contains("不得编造商品、优惠、库存、物流、售后")
                .contains("全部正文合计不得超过200字", "引导问题不计入该字数限制")
                .doesNotContain("{{platform}}");
    }

    @Test
    void acceptsExpectedTwoSectionOutput() {
        service.validateOutput("直接复制话术（淘宝版）：\n“亲，欢迎光临，请问您主要在什么场景使用呢？”\n\n"
                + "话术点评：\n用欢迎语降低沟通门槛，并用场景问题继续了解需求。");
    }

    @Test
    void rejectsOutputWithoutCommentary() {
        assertThrows(DeepSeekClient.DeepSeekApiException.class,
                () -> service.validateOutput("直接复制话术（淘宝版）：\n“亲，请问您主要在什么场景使用呢？”"));
    }

    @Test
    void rejectsJsonOutput() {
        assertThrows(DeepSeekClient.DeepSeekApiException.class,
                () -> service.validateOutput("{\"直接复制话术\":\"您好\",\"话术点评\":\"欢迎语\"}"));
    }
}
