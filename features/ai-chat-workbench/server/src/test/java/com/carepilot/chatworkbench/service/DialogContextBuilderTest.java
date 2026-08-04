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
                第3轮：第三问

                回复策略历史：
                第1轮：第一轮策略
                第2轮：第二轮策略
                第3轮：第三轮策略""");
        assertThat(contexts.get(2)).isEqualTo("""
                用户问题历史：
                第1轮：第一问
                第2轮：第二问
                第3轮：第三问

                推荐话术历史：
                第3轮：第三轮话术""");
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
    void storedContextContainsAllQuestionsAndOnlySameStepOutputsWithoutKeywords() {
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
                第3轮：第三问

                回复策略历史：
                第1轮：第一轮策略
                第2轮：第二轮第一次策略
                第2轮（第2次生成）：第二轮第二次策略
                第3轮：第三轮策略""");
        assertThat(context)
                .doesNotContain("关键词")
                .doesNotContain("第一轮意图")
                .doesNotContain("第二轮意图");
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
