package com.carepilot.chatworkbench.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReplyStrategyRuleServiceTest {

    private final ReplyStrategyRuleService service = new ReplyStrategyRuleService();

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
        assertThat(plan.prompt()).contains("热情破冰", "选择具体平台可提升策略精准度");
        assertThat(service.validate("先欢迎客户，再询问主要是通勤还是游戏使用。", plan).valid()).isTrue();
        assertThat(service.validate("我们会全程物流跟踪，今天下单今天发。", plan).valid()).isFalse();
    }

    @Test
    void rejectsMissingOrVagueCarrierForLogisticsConcern() {
        assertThatThrownBy(() -> service.plan("拼多多", "怕快递摔坏", "物流顾虑", null))
                .hasMessageContaining("填写具体合作快递名称");
        assertThatThrownBy(() -> service.plan("拼多多", "怕快递摔坏", "物流顾虑", "随机快递"))
                .hasMessageContaining("具体合作快递名称");
    }
}
