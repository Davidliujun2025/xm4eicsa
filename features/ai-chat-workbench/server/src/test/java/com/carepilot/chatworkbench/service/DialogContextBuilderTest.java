package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DialogContextBuilderTest {

    private final DialogContextBuilder builder = new DialogContextBuilder();

    @Test
    void buildsStepSpecificContextFromConversationHistoryAndCurrentReply() {
        List<AiDialogStepRecord> historyRecords = List.of(
                stepRecord(1, 1, "第一问", "第一轮意图"),
                stepRecord(1, 2, "第一问", "第一轮策略"),
                stepRecord(2, 1, "第二问", "第二轮意图"),
                stepRecord(2, 2, "第二问", "第二轮策略")
        );

        List<String> contexts = builder.buildStepContexts(historyRecords, "第三问", List.of(
                "第三轮意图",
                "第三轮策略",
                "第三轮话术",
                "第三轮钩子",
                "第三轮收尾"
        ));

        assertThat(contexts).hasSize(5);
        assertThat(contexts.get(1)).isEqualTo("""
                用户问题历史：
                第1轮：第一问
                第2轮：第二问

                回复策略历史：
                第1轮：第一轮策略
                第2轮：第二轮策略""");
        assertThat(contexts.get(2)).isEqualTo("""
                用户问题历史：
                第1轮：第一问
                第2轮：第二问

                推荐话术历史：
                无""");
    }

    @Test
    void buildsApiContextFromPreviousQuestionsAndIntentResultsOnly() {
        List<AiDialogStepRecord> historyRecords = List.of(
                stepRecord(1, 1, "第一问", "第一轮意图"),
                stepRecord(1, 2, "第一问", "第一轮策略"),
                stepRecord(2, 1, "第二问", "第二轮意图")
        );

        String context = builder.buildConversationContext(historyRecords);

        assertThat(context).isEqualTo("""
                用户问题历史：
                第1轮：第一问
                第2轮：第二问

                意图识别历史：
                第1轮：第一轮意图
                第2轮：第二轮意图""");
        assertThat(context).doesNotContain("第一轮策略");
    }

    @Test
    void storedContextContainsPreviousQuestionsAndSameStepOutputsWithoutCurrentRoundOrKeywords() {
        List<AiDialogStepRecord> historyRecords = List.of(
                stepRecord(1, 1, 1, "第一问", "关键词：不应写入数据库上下文"),
                stepRecord(1, 2, 1, "第一问", "第一轮策略"),
                stepRecord(2, 1, 1, "第二问", "第二轮意图"),
                stepRecord(2, 2, 1, "第二问", "第二轮第一次策略"),
                stepRecord(2, 2, 2, "第二问", "第二轮第二次策略")
        );

        String context = builder.buildStoredStepContext(
                historyRecords, "第三问", 3, 2, 1, "第三轮策略");

        assertThat(context).isEqualTo("""
                用户问题历史：
                第1轮：第一问
                第2轮：第二问

                回复策略历史：
                第1轮：第一轮策略
                第2轮：第二轮第一次策略
                第2轮（第2次生成）：第二轮第二次策略""");
        assertThat(context)
                .doesNotContain("关键词")
                .doesNotContain("第一轮意图")
                .doesNotContain("第二轮意图")
                .doesNotContain("第三问")
                .doesNotContain("第三轮策略");
    }

    @Test
    void keepsOnlyRoundsTwoThroughSixAsContextForRoundSeven() {
        List<AiDialogStepRecord> historyRecords = List.of(
                stepRecord(1, 2, "第一问", "第一轮策略"),
                stepRecord(2, 2, "第二问", "第二轮策略"),
                stepRecord(3, 2, "第三问", "第三轮策略"),
                stepRecord(4, 2, "第四问", "第四轮策略"),
                stepRecord(5, 2, "第五问", "第五轮策略"),
                stepRecord(6, 2, 1, "第六问", "第六轮第一次策略"),
                stepRecord(6, 2, 2, "第六问", "第六轮第二次策略"),
                stepRecord(7, 2, "第七问", "第七轮旧策略")
        );

        String context = builder.buildStoredStepContext(
                historyRecords, "第七问", 7, 2, 2, "第七轮新策略");

        assertThat(context)
                .contains("第2轮：第二问", "第6轮：第六问")
                .contains("第2轮：第二轮策略", "第6轮（第2次生成）：第六轮第二次策略")
                .doesNotContain("第1轮：", "第一问", "第一轮策略")
                .doesNotContain("第7轮：", "第七问", "第七轮旧策略", "第七轮新策略");
    }

    @Test
    void intentApiContextUsesOnlyTheFiveRoundsBeforeTheCurrentRound() {
        List<AiDialogStepRecord> historyRecords = List.of(
                stepRecord(1, 1, "第一问", "第一轮意图"),
                stepRecord(2, 1, "第二问", "第二轮意图"),
                stepRecord(3, 1, "第三问", "第三轮意图"),
                stepRecord(4, 1, "第四问", "第四轮意图"),
                stepRecord(5, 1, "第五问", "第五轮意图"),
                stepRecord(6, 1, "第六问", "第六轮意图"),
                stepRecord(7, 1, "第七问", "第七轮旧意图")
        );

        String context = builder.buildConversationContext(historyRecords, 7);

        assertThat(context)
                .contains("第2轮：第二问", "第6轮：第六问")
                .contains("第2轮：第二轮意图", "第6轮：第六轮意图")
                .doesNotContain("第1轮：", "第一问", "第一轮意图")
                .doesNotContain("第7轮：", "第七问", "第七轮旧意图");
    }

    private AiDialogStepRecord stepRecord(int dialogRound, int stepNo, String question, String content) {
        return stepRecord(dialogRound, stepNo, 1, question, content);
    }

    private AiDialogStepRecord stepRecord(int dialogRound, int stepNo, int stepRound,
                                          String question, String content) {
        return AiDialogStepRecord.builder()
                .dialogRound(dialogRound)
                .stepNo((byte) stepNo)
                .stepRound(stepRound)
                .customerDialog(question)
                .aiContent(content)
                .build();
    }
}
