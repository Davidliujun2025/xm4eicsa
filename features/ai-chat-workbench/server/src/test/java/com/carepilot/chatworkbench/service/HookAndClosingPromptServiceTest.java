package com.carepilot.chatworkbench.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HookAndClosingPromptServiceTest {

    private final HookAndClosingPromptService service = new HookAndClosingPromptService();

    @Test
    void loadsHookPromptWithRequiredSectionsAndSafetyRules() {
        assertThat(service.render(4))
                .contains("第四步：钩子引导")
                .contains("钩子话术内容", "理由说明", "预期效果")
                .contains("全部正文合计不得超过200字", "引导问题不计入该字数限制")
                .contains("不得把示例中的权益复制到实际输出");
    }

    @Test
    void loadsClosingPromptWithConditionalOrderStatus() {
        assertThat(service.render(5))
                .contains("第五步：成功收尾")
                .contains("尚未确认消费者已经下单")
                .contains("预生成收尾话术（消费者拍下后发送）")
                .contains("全部正文合计不得超过200字", "引导问题不计入该字数限制")
                .contains("只有上下文明确确认客户已经下单时");
    }

    @Test
    void validatesHookOutput() {
        service.validateOutput(4, "钩子话术内容（接在推荐话术后，同一句或下一句发送）：\n“您更关注使用场景还是预算呢？”"
                + "\n\n理由说明：用低门槛问题继续了解需求。\n\n预期效果：鼓励客户补充关键信息。");
    }

    @Test
    void validatesPreGeneratedClosingOutput() {
        service.validateOutput(5, "备注：当前尚未确认下单，以下内容确认订单后再发送。\n\n"
                + "预生成收尾话术（消费者拍下后发送）：\n“感谢您的选择，有问题随时联系我们。”");
    }

    @Test
    void rejectsHookWithoutReason() {
        assertThrows(DeepSeekClient.DeepSeekApiException.class,
                () -> service.validateOutput(4, "钩子话术内容：您好。\n预期效果：继续沟通。"));
    }
}
