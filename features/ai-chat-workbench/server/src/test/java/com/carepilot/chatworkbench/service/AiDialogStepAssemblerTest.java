package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.dto.response.ChatMessageResponse;
import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiDialogStepAssemblerTest {

    private final AiDialogStepAssembler assembler = new AiDialogStepAssembler(new ObjectMapper());

    @Test
    void assemblesFiveStepRowsIntoOneConversationTurnWithoutDuplicatingTokens() {
        List<AiDialogStepRecord> records = new ArrayList<>();
        for (int stepNo = 1; stepNo <= 5; stepNo++) {
            records.add(AiDialogStepRecord.builder()
                    .recordId((long) stepNo)
                    .sessionTaskId(10L)
                    .customerDialog("客户问题")
                    .dialogRound(1)
                    .stepNo((byte) stepNo)
                    .stepRound(1)
                    .aiContent("step-" + stepNo)
                    .extraJson(assembler.createExtraJson("售后咨询", "风险提示", "处理建议"))
                    .promptTokens(stepNo == 1 ? 120 : 0)
                    .completionTokens(stepNo == 1 ? 380 : 0)
                    .totalTokens(stepNo == 1 ? 500 : 0)
                    .createTime(LocalDateTime.of(2026, 8, 1, 10, 0))
                    .build());
        }

        List<ChatMessageResponse> messages = assembler.toMessages(records);

        assertThat(messages).hasSize(1);
        ChatMessageResponse message = messages.getFirst();
        assertThat(message.getDialogRound()).isEqualTo(1);
        assertThat(message.getQuestion()).isEqualTo("客户问题");
        assertThat(message.getCustomerType()).isEqualTo("售后咨询");
        assertThat(message.getIntentRecognition()).isEqualTo("step-1");
        assertThat(message.getSuccessClose()).isEqualTo("step-5");
        assertThat(message.getRiskWarning()).isEqualTo("风险提示");
        assertThat(message.getRiskSuggestion()).isEqualTo("处理建议");
        assertThat(message.getPromptTokens()).isEqualTo(120);
        assertThat(message.getCompletionTokens()).isEqualTo(380);
        assertThat(message.getTotalTokens()).isEqualTo(500);
    }
}
