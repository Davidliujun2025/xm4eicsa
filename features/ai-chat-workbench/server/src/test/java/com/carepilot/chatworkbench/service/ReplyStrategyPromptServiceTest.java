package com.carepilot.chatworkbench.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplyStrategyPromptServiceTest {

    private final ReplyStrategyPromptService service = new ReplyStrategyPromptService();

    @Test
    void loadsTheReplyStrategyDocumentAsTheSystemPrompt() {
        assertThat(service.render())
                .contains("第二步：回复策略")
                .contains("结合第1步意图识别的结果")
                .contains("输出正文总字数限制在200字以内")
                .contains("【回复策略】", "【意图识别】", "【策略点评】");
    }

    @Test
    void acceptsPlainTextStrategyOutput() {
        service.validateOutput("【回复策略】\n先共情并确认诉求。\n\n【策略点评】\n承接意图识别结果。");
    }

    @Test
    void rejectsJsonOutput() {
        assertThrows(DeepSeekClient.DeepSeekApiException.class,
                () -> service.validateOutput("{\"replyStrategy\":\"先共情\"}"));
    }
}
