package com.carepilot.chatworkbench.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReplyStrategyRuleServiceTest {

    private final ReplyStrategyRuleService service = new ReplyStrategyRuleService();
    @Test
    void intentTextMentioningLogisticsDoesNotTriggerConcernWhenCustomerQuestionIsClean() {
        // AC13：意图识别引导问题中的“发货”等泛化措辞不得把纯寒暄误判为物流顾虑
        assertThat(service.hasLogisticsConcern("在吗", "引导问题：2. 您主要关注价格、功能，还是发货和售后方面？"))
                .isFalse();
        ReplyStrategyRuleService.ReplyStrategyPlan plan = service.plan(
                "抖音", "在吗", "引导问题：2. 您主要关注价格、功能，还是发货和售后方面？", null);
        assertThat(plan.logisticsConcern()).isFalse();
        assertThat(plan.prompt()).contains("热情破冰", "回复策略：", "回复理由：", "预期效果：", "对转化的影响：")
                .contains("全部正文合计不得超过200字", "引导问题不计入该字数限制");
    }

    @Test
    void customerQuestionWithLogisticsWordStillTriggersConcern() {
        assertThat(service.hasLogisticsConcern("你们发什么快递", "任意意图分析文本")).isTrue();
        assertThat(service.hasLogisticsConcern("怕快递把东西摔坏", "任意意图分析文本")).isTrue();
    }

    @Test
    void logisticsConcernRequiresSpecificCarrierAndValidatesAllFourElementsInOrder() {
        ReplyStrategyRuleService.ReplyStrategyPlan plan = service.plan(
                "淘宝", "XX快递太慢了，怕把耳机摔坏", "客户对物流时效和破损有顾虑", "顺丰");

        String valid = "确实理解您担心快递慢和运输磕碰。我们这次固定发顺丰。"
                + "运输破损我们包赔，万一丢件免费补发，并且全程跟踪物流、异常及时处理。"
                + "您今天下单我们今天就安排发货。";

        assertThat(plan.logisticsConcern()).isTrue();
        assertThat(service.validate(valid, plan).valid()).isTrue();
    }

    @Test
    void logisticsStrategyRejectsMissingGuaranteeOrWrongOrderOrCourierDefence() {
        ReplyStrategyRuleService.ReplyStrategyPlan plan = service.plan(
                "京东", "发什么快递？", "客户询问物流", "中通");

        String invalid = "我们发中通，快递很好，今天下单今天发。理解您的顾虑，破损包赔。";

        ReplyStrategyRuleService.ValidationResult result = service.validate(invalid, plan);
        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("丢件补发", "全程物流跟踪", "禁用表达", "顺序");
    }

    @Test
    void nonLogisticsConcernUsesIcebreakerAndRejectsLogisticsPromises() {
        ReplyStrategyRuleService.ReplyStrategyPlan plan = service.plan(
                "其他平台", "这款耳机适合打游戏吗", "客户在咨询商品功能", null);

        assertThat(plan.logisticsConcern()).isFalse();
        assertThat(plan.prompt()).contains("热情破冰", "选择具体平台可提升策略精准度")
                .contains("不直接推品", "使用场景", "连接设备", "将被动等待转化为主动引导");
        assertThat(service.validate("先欢迎客户，再询问主要是通勤还是游戏使用。", plan).valid()).isTrue();
        assertThat(service.validate("我们会全程物流跟踪，今天下单今天发。", plan).valid()).isFalse();
    }

    @Test
    void acceptsSemanticallyEquivalentUrgencyHooks() {
        ReplyStrategyRuleService.ReplyStrategyPlan plan = service.plan(
                "拼多多", "发什么快递？", "客户询问物流", "中通");
        String[] variants = {
                "理解您的担心。我们发中通，破损包赔、丢件补发、全程跟踪。今天就能发出。",
                "理解您的担心。我们发中通，破损包赔、丢件补发、全程跟踪。现在下单，今天就能发货。",
                "理解您的担心。我们发中通，破损包赔、丢件补发、全程跟踪。马上安排发货。",
                "理解您的担心。我们发中通，破损包赔、丢件补发、全程跟踪。尽快发出。",
                "理解您的担心。我们发中通，破损包赔、丢件补发、全程跟踪。拍下后当天发出。",
        };
        for (String content : variants) {
            assertThat(service.validate(content, plan).valid())
                    .as("should accept: " + content)
                    .isTrue();
        }
    }

    @Test
    void rejectsMissingOrVagueCarrierForLogisticsConcern() {
        assertThatThrownBy(() -> service.plan("拼多多", "怕快递摔坏", "物流顾虑", null))
                .hasMessageContaining("填写具体合作快递名称");
        assertThatThrownBy(() -> service.plan("拼多多", "怕快递摔坏", "物流顾虑", "随机快递"))
                .hasMessageContaining("具体合作快递名称");
    }
}
