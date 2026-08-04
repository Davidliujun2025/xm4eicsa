package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.dto.response.ConversationResponse;
import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import com.carepilot.chatworkbench.entity.Conversation;
import com.carepilot.chatworkbench.repository.AiDialogStepRecordRepository;
import com.carepilot.chatworkbench.repository.ConversationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIServiceRegenerationTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private AiDialogStepRecordRepository stepRecordRepository;
    @Mock
    private AiDialogStepAssembler stepAssembler;
    @Mock
    private ConversationService conversationService;
    @Mock
    private TokenService tokenService;
    @Mock
    private DeepSeekClient deepSeekClient;

    private AIService service;

    @BeforeEach
    void setUp() {
        service = new AIService(
                conversationRepository,
                stepRecordRepository,
                stepAssembler,
                new DialogContextBuilder(),
                conversationService,
                tokenService,
                deepSeekClient,
                new IntentRecognitionPromptService());
        ReflectionTestUtils.setField(service, "timeoutSeconds", 5);
    }

    @AfterEach
    void shutDownExecutor() {
        ExecutorService executor = (ExecutorService) ReflectionTestUtils.getField(service, "executorService");
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void regeneratingStepTwoAddsOnlyStepRoundTwoForTheSameDialogAndStep() {
        Conversation conversation = Conversation.builder()
                .id(10L)
                .conversationId("conversation-1")
                .customerId("6")
                .platform("拼多多")
                .build();
        AiDialogStepRecord intent = record(1, 3, 1, 1, "六维意图识别");
        AiDialogStepRecord currentStrategy = record(2, 3, 2, 1, "第一次回复策略");
        ConversationResponse expected = ConversationResponse.builder()
                .conversationId("conversation-1")
                .build();

        when(conversationRepository.findByConversationId("conversation-1"))
                .thenReturn(Optional.of(conversation));
        when(stepRecordRepository
                .findBySessionTaskIdAndDialogRoundAndStepNoAndIsDeleteOrderByStepRoundDesc(
                        10L, 3, (byte) 2, (byte) 0))
                .thenReturn(List.of(currentStrategy));
        when(stepRecordRepository
                .findBySessionTaskIdAndIsEffectiveAndIsDeleteOrderByDialogRoundAscStepNoAsc(
                        10L, (byte) 1, (byte) 0))
                .thenReturn(List.of(intent, currentStrategy));
        when(stepRecordRepository
                .findBySessionTaskIdAndIsDeleteOrderByDialogRoundAscStepNoAscStepRoundAsc(
                        10L, (byte) 0))
                .thenReturn(List.of(intent, currentStrategy));
        when(deepSeekClient.complete(anyString(), anyString()))
                .thenReturn(new DeepSeekClient.DeepSeekResult(
                        "第二次回复策略", 80, 40, 120,
                        "deepseek-request-2", "deepseek-v4-flash"));
        when(conversationService.getConversationById("6", "conversation-1"))
                .thenReturn(expected);

        ConversationResponse actual = service.regenerateStep("6", "conversation-1", 3, 2);

        assertThat(actual).isSameAs(expected);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<AiDialogStepRecord>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(stepRecordRepository).saveAll(captor.capture());
        List<AiDialogStepRecord> saved = ((List<AiDialogStepRecord>) captor.getValue());
        assertThat(saved).hasSize(2);
        assertThat(saved.getFirst().getIsEffective()).isZero();
        AiDialogStepRecord regenerated = saved.get(1);
        assertThat(regenerated.getDialogRound()).isEqualTo(3);
        assertThat(regenerated.getStepNo()).isEqualTo((byte) 2);
        assertThat(regenerated.getStepRound()).isEqualTo(2);
        assertThat(regenerated.getAiContent()).isEqualTo("第二次回复策略");
        assertThat(regenerated.getIsEffective()).isEqualTo((byte) 1);
        assertThat(regenerated.getPromptTokens()).isEqualTo(80);
        assertThat(regenerated.getCompletionTokens()).isEqualTo(40);
        assertThat(regenerated.getTotalTokens()).isEqualTo(120);
        assertThat(regenerated.getDialogContext())
                .contains("用户问题历史：", "回复策略历史：", "第一次回复策略", "第二次回复策略")
                .doesNotContain("六维意图识别", "关键词");
    }

    @Test
    void creatingConversationCallsDeepSeekOnceAndPersistsOnlyIntentStep() {
        ConversationResponse expected = ConversationResponse.builder()
                .conversationId("generated-conversation")
                .build();
        String intent = "消费者情绪/心情：平和\n\n"
                + "进店理由/场景：咨询商品\n\n"
                + "痛点/爽点：尚无法确认\n\n"
                + "购买意向：有初步意向\n\n"
                + "性格/决策链路：尚无法确认\n\n"
                + "结论：需要继续了解需求";

        doAnswer(invocation -> {
            Conversation saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        }).when(conversationRepository).saveAndFlush(any(Conversation.class));
        when(stepRecordRepository.findMaxDialogRound(10L)).thenReturn(0);
        when(stepRecordRepository
                .findBySessionTaskIdAndIsEffectiveAndIsDeleteOrderByDialogRoundAscStepNoAsc(
                        10L, (byte) 1, (byte) 0))
                .thenReturn(List.of());
        when(stepAssembler.createExtraJson(anyString(), anyString(), anyString()))
                .thenReturn("{}");
        when(deepSeekClient.recognizeIntent(anyString()))
                .thenReturn(new DeepSeekClient.DeepSeekResult(
                        intent, 100, 50, 150,
                        "deepseek-intent-1", "deepseek-v4-flash"));
        when(conversationService.getConversationById(anyString(), anyString()))
                .thenReturn(expected);

        ConversationResponse actual = service.createConversation(
                "6", "这个商品有货吗", "拼多多", "直接咨询");

        assertThat(actual).isSameAs(expected);
        ArgumentCaptor<AiDialogStepRecord> captor = ArgumentCaptor.forClass(AiDialogStepRecord.class);
        verify(stepRecordRepository).save(captor.capture());
        verify(deepSeekClient, times(1)).recognizeIntent(anyString());
        AiDialogStepRecord saved = captor.getValue();
        assertThat(saved.getDialogRound()).isEqualTo(1);
        assertThat(saved.getStepNo()).isEqualTo((byte) 1);
        assertThat(saved.getStepRound()).isEqualTo(1);
        assertThat(saved.getAiContent()).isEqualTo(intent);
        assertThat(saved.getTotalTokens()).isEqualTo(150);
        assertThat(saved.getDialogContext())
                .contains("用户问题历史：", "这个商品有货吗", "意图识别历史：", intent)
                .doesNotContain("关键词", "角色", "平台规则");
    }

    @Test
    void generatingNextStepCallsDeepSeekOnceAndPersistsThatStepImmediately() {
        Conversation conversation = Conversation.builder()
                .id(10L)
                .conversationId("conversation-1")
                .customerId("6")
                .platform("拼多多")
                .build();
        AiDialogStepRecord intent = record(1, 3, 1, 1, "六维意图识别");
        ConversationResponse expected = ConversationResponse.builder()
                .conversationId("conversation-1")
                .build();

        when(conversationRepository.findByConversationId("conversation-1"))
                .thenReturn(Optional.of(conversation));
        when(stepRecordRepository
                .findBySessionTaskIdAndDialogRoundAndStepNoAndIsDeleteOrderByStepRoundDesc(
                        10L, 3, (byte) 2, (byte) 0))
                .thenReturn(List.of());
        when(stepRecordRepository
                .findBySessionTaskIdAndIsEffectiveAndIsDeleteOrderByDialogRoundAscStepNoAsc(
                        10L, (byte) 1, (byte) 0))
                .thenReturn(List.of(intent));
        when(stepRecordRepository
                .findBySessionTaskIdAndIsDeleteOrderByDialogRoundAscStepNoAscStepRoundAsc(
                        10L, (byte) 0))
                .thenReturn(List.of(intent));
        when(deepSeekClient.complete(anyString(), anyString()))
                .thenReturn(new DeepSeekClient.DeepSeekResult(
                        "回复策略内容", 70, 30, 100,
                        "deepseek-step-2", "deepseek-v4-flash"));
        when(conversationService.getConversationById("6", "conversation-1"))
                .thenReturn(expected);

        ConversationResponse actual = service.generateStep("6", "conversation-1", 3, 2);

        assertThat(actual).isSameAs(expected);
        ArgumentCaptor<AiDialogStepRecord> captor = ArgumentCaptor.forClass(AiDialogStepRecord.class);
        verify(stepRecordRepository).save(captor.capture());
        verify(deepSeekClient, times(1)).complete(anyString(), anyString());
        AiDialogStepRecord saved = captor.getValue();
        assertThat(saved.getDialogRound()).isEqualTo(3);
        assertThat(saved.getStepNo()).isEqualTo((byte) 2);
        assertThat(saved.getStepRound()).isEqualTo(1);
        assertThat(saved.getAiContent()).isEqualTo("回复策略内容");
        assertThat(saved.getIsEffective()).isEqualTo((byte) 1);
        assertThat(saved.getTotalTokens()).isEqualTo(100);
        assertThat(saved.getDialogContext())
                .contains("用户问题历史：", "这个商品有货吗", "回复策略历史：", "回复策略内容")
                .doesNotContain("六维意图识别", "关键词");
    }

    private AiDialogStepRecord record(int recordId, int dialogRound, int stepNo,
                                      int stepRound, String content) {
        return AiDialogStepRecord.builder()
                .recordId((long) recordId)
                .sessionTaskId(10L)
                .platformId(3L)
                .customerDialog("这个商品有货吗")
                .dialogRound(dialogRound)
                .stepNo((byte) stepNo)
                .stepRound(stepRound)
                .aiContent(content)
                .extraJson("{}")
                .generateStatus((byte) 1)
                .isEffective((byte) 1)
                .isDelete((byte) 0)
                .build();
    }
}
