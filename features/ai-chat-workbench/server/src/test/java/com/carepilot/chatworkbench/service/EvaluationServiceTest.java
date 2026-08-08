package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import com.carepilot.chatworkbench.entity.Conversation;
import com.carepilot.chatworkbench.entity.CustomerServiceEvaluationReport;
import com.carepilot.chatworkbench.repository.AiDialogStepRecordRepository;
import com.carepilot.chatworkbench.repository.ConversationRepository;
import com.carepilot.chatworkbench.repository.CustomerServiceEvaluationReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock ConversationRepository conversationRepository;
    @Mock AiDialogStepRecordRepository stepRecordRepository;
    @Mock CustomerServiceEvaluationReportRepository reportRepository;
    @Mock DeepSeekClient deepSeekClient;
    @Mock TokenService tokenService;
    @Mock JdbcTemplate jdbcTemplate;
    private EvaluationService service;

    @BeforeEach
    void setUp() {
        service = new EvaluationService(conversationRepository, stepRecordRepository, reportRepository,
                deepSeekClient, tokenService, jdbcTemplate, new ObjectMapper(),
                20, 25, 15, 20, 15, 5);
    }

    @Test
    void generatesWeightedReportFromCompleteFiveStepConversation() {
        Conversation conversation = Conversation.builder().id(10L).conversationId("c-1")
                .customerId("6").platform("淘宝").title("咨询").build();
        List<AiDialogStepRecord> records = List.of(
                record(1, "意图识别"), record(2, "回复策略"), record(3, "推荐话术"),
                record(4, "钩子引导"), record(5, "成功收尾，未出现违规承诺"));
        when(reportRepository.findByConversationId("c-1")).thenReturn(Optional.empty());
        when(conversationRepository.findByConversationId("c-1")).thenReturn(Optional.of(conversation));
        when(stepRecordRepository.findBySessionTaskIdAndIsEffectiveAndIsDeleteOrderByDialogRoundAscStepNoAsc(
                10L, (byte) 1, (byte) 0)).thenReturn(records);
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), any(Object[].class)))
                .thenReturn(List.of("违规"));
        when(deepSeekClient.complete(anyString(), anyString())).thenReturn(new DeepSeekClient.DeepSeekResult(
                """
                {"serviceAttitude":90,"problemSolving":80,"empathy":70,"compliance":100,
                 "conversionGuidance":60,"responseEfficiency":50,
                 "strengths":"态度友好","problems":"引导仍可优化","suggestions":"进一步确认需求"}
                """, 120, 80, 200, "eval-1", "deepseek-test"));
        when(tokenService.getSessionUsedTokens(10L)).thenReturn(500);
        when(reportRepository.save(any(CustomerServiceEvaluationReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerServiceEvaluationReport report = service.generateForCompletedConversation("6", "c-1");

        assertThat(report.getTotalScore()).isEqualTo(80);
        assertThat(report.getForbiddenHitCount()).isEqualTo(1);
        assertThat(report.getForbiddenWordsJson()).contains("违规");
        assertThat(report.getTranscript()).contains("第1轮客户问题", "第5步AI输出");
        verify(tokenService).recordEvaluationUsage(eq("6"), eq("c-1"), any());
    }

    @Test
    void returnsExistingReportWithoutCallingDeepSeekAgain() {
        CustomerServiceEvaluationReport existing = CustomerServiceEvaluationReport.builder()
                .id(8L).conversationId("c-1").operatorId("6").build();
        when(reportRepository.findByConversationId("c-1")).thenReturn(Optional.of(existing));

        assertThat(service.generateForCompletedConversation("6", "c-1")).isSameAs(existing);
        verifyNoInteractions(deepSeekClient);
    }

    private AiDialogStepRecord record(int step, String content) {
        LocalDateTime now = LocalDateTime.now();
        return AiDialogStepRecord.builder().recordId((long) step).sessionTaskId(10L)
                .customerDialog("客户问题").dialogRound(1).stepNo((byte) step).stepRound(1)
                .aiContent(content).isEffective((byte) 1).isDelete((byte) 0).generateStatus((byte) 1)
                .triggerAt(now.minusSeconds(1)).llmReplyAt(now).promptTokens(10).completionTokens(10)
                .totalTokens(20).build();
    }
}
