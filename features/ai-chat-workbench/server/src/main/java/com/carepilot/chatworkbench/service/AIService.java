package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.dto.response.ConversationResponse;
import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import com.carepilot.chatworkbench.entity.Conversation;
import com.carepilot.chatworkbench.enums.PlatformEnum;
import com.carepilot.chatworkbench.repository.AiDialogStepRecordRepository;
import com.carepilot.chatworkbench.repository.ConversationRepository;
import com.carepilot.chatworkbench.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final ConversationRepository conversationRepository;
    private final AiDialogStepRecordRepository stepRecordRepository;
    private final AiDialogStepAssembler stepAssembler;
    private final ConversationService conversationService;
    private final TokenService tokenService;

    @Value("${app.ai.timeout-seconds:15}")
    private Integer timeoutSeconds;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Transactional
    public ConversationResponse createConversation(String customerId, String question,
                                                    String platform, String customerType) {
        assertTokenQuota(customerId);

        Conversation conversation = Conversation.builder()
                .conversationId(IdGenerator.generateConversationId())
                .customerId(customerId)
                .platform(platform)
                .title(truncate(question, 50))
                .build();
        conversationRepository.saveAndFlush(conversation);

        AIResponseResult result = generateAIResponse(question, platform);
        saveGeneration(conversation, customerId, question, customerType, result);
        return conversationService.getConversationById(customerId, conversation.getConversationId());
    }

    @Transactional
    public ConversationResponse addMessage(String customerId, String conversationId,
                                           String question, String customerType) {
        assertTokenQuota(customerId);

        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation does not exist"));
        if (!customerId.equals(conversation.getCustomerId())) {
            throw new SecurityException("Access to this conversation is forbidden");
        }

        AIResponseResult result = generateAIResponse(question, conversation.getPlatform());
        saveGeneration(conversation, customerId, question, customerType, result);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);
        return conversationService.getConversationById(customerId, conversationId);
    }

    private void saveGeneration(Conversation conversation, String customerId, String question,
                                String customerType, AIResponseResult result) {
        int dialogRound = stepRecordRepository.findMaxDialogRound(conversation.getId()) + 1;
        LocalDateTime triggerAt = LocalDateTime.now();
        LocalDateTime replyAt = LocalDateTime.now();
        String traceId = UUID.randomUUID().toString();
        String extraJson = stepAssembler.createExtraJson(
                customerType, result.riskWarning(), result.riskSuggestion());
        List<String> contents = List.of(
                result.intentRecognition(),
                result.replyStrategy(),
                result.recommendedScript(),
                result.hookGuidance(),
                result.successClose());

        List<AiDialogStepRecord> records = new ArrayList<>(5);
        for (int index = 0; index < contents.size(); index++) {
            int stepNo = index + 1;
            boolean tokenOwner = stepNo == 1;
            records.add(AiDialogStepRecord.builder()
                    .sessionTaskId(conversation.getId())
                    .platformId(platformId(conversation.getPlatform()))
                    .customerDialog(question)
                    .dialogRound(dialogRound)
                    .stepNo((byte) stepNo)
                    .stepRound(1)
                    .aiContent(contents.get(index))
                    .extraJson(extraJson)
                    .isManualEdit((byte) 0)
                    .isEffective((byte) 1)
                    .triggerAt(triggerAt)
                    .llmReplyAt(replyAt)
                    .generateStatus((byte) 1)
                    .operatorId(Long.valueOf(customerId))
                    .traceId(traceId)
                    .isDelete((byte) 0)
                    .promptTokens(tokenOwner ? result.promptTokens() : 0)
                    .completionTokens(tokenOwner ? result.completionTokens() : 0)
                    .totalTokens(tokenOwner ? result.totalTokens() : 0)
                    .build());
        }
        stepRecordRepository.saveAll(records);
    }

    private AIResponseResult generateAIResponse(String question, String platform) {
        CompletableFuture<AIResponseResult> future = CompletableFuture.supplyAsync(
                () -> simulateAIResponse(question, platform), executorService);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception exception) {
            future.cancel(true);
            log.error("AI generation timeout or failure after {} seconds", timeoutSeconds, exception);
            throw new RuntimeException("AI generation failed", exception);
        }
    }

    private AIResponseResult simulateAIResponse(String question, String platform) {
        int promptTokens = (question.length() + 500) / 4;
        int completionTokens = 300 + (int) (Math.random() * 200);
        return new AIResponseResult(
                "客户意图：售前咨询\n关键词：" + extractKeywords(question),
                "先明确客户需求，再根据平台规则给出专业建议，最后引导客户继续咨询或下单。",
                String.format("您好！感谢您的咨询。针对您提出的“%s”问题，在%s平台上我们会为您提供专业服务支持。",
                        truncate(question, 30), platform),
                "可补充当前活动优惠和售后保障，增强客户下单信心。",
                "您可以放心咨询，如果后续有任何问题，我们也会继续协助您处理。",
                "当前平台：" + platform,
                "建议使用中性描述，并在回复前核对平台最新规则。",
                promptTokens,
                completionTokens);
    }

    private void assertTokenQuota(String customerId) {
        if (tokenService.isTokenQuotaExceeded(customerId)) {
            throw new IllegalStateException("今日Token配额已用尽");
        }
    }

    private Long platformId(String platform) {
        PlatformEnum[] platforms = PlatformEnum.values();
        for (int index = 0; index < platforms.length; index++) {
            PlatformEnum candidate = platforms[index];
            if (candidate.name().equalsIgnoreCase(platform)
                    || candidate.getDescription().equalsIgnoreCase(platform)) {
                return (long) index + 1;
            }
        }
        return null;
    }

    private String extractKeywords(String question) {
        return question.length() < 10
                ? question
                : question.substring(0, Math.min(question.length(), 20)) + "...";
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private record AIResponseResult(
            String intentRecognition,
            String replyStrategy,
            String recommendedScript,
            String hookGuidance,
            String successClose,
            String riskWarning,
            String riskSuggestion,
            Integer promptTokens,
            Integer completionTokens
    ) {
        Integer totalTokens() {
            return promptTokens + completionTokens;
        }
    }
}
