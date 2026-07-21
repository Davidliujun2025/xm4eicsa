package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.dto.response.ChatMessageResponse;
import com.carepilot.chatworkbench.dto.response.ConversationResponse;
import com.carepilot.chatworkbench.dto.response.TokenInfo;
import com.carepilot.chatworkbench.entity.ChatMessage;
import com.carepilot.chatworkbench.entity.Conversation;
import com.carepilot.chatworkbench.repository.ChatMessageRepository;
import com.carepilot.chatworkbench.repository.ConversationRepository;
import com.carepilot.chatworkbench.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TokenService tokenService;

    @Value("${app.ai.timeout-seconds:15}")
    private Integer timeoutSeconds;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    @Transactional
    public ConversationResponse createConversation(String customerId, String question,
                                                    String platform, String customerType) {
        if (tokenService.isTokenQuotaExceeded(customerId)) {
            throw new IllegalStateException("今日Token配额已耗尽");
        }

        String conversationId = IdGenerator.generateConversationId();
        String title = truncate(question, 50);

        AIResponseResult result = generateAIResponse(question, platform);

        Conversation conversation = Conversation.builder()
                .conversationId(conversationId)
                .customerId(customerId)
                .platform(platform)
                .title(title)
                .build();
        conversationRepository.save(conversation);

        ChatMessage message = ChatMessage.builder()
                .conversationId(conversationId)
                .question(question)
                .customerType(customerType)
                .intentRecognition(result.intentRecognition)
                .replyStrategy(result.replyStrategy)
                .recommendedScript(result.recommendedScript)
                .hookGuidance(result.hookGuidance)
                .successClose(result.successClose)
                .riskWarning(result.riskWarning)
                .riskSuggestion(result.riskSuggestion)
                .totalTokens(result.totalTokens)
                .promptTokens(result.promptTokens)
                .completionTokens(result.completionTokens)
                .build();
        chatMessageRepository.save(message);

        tokenService.recordTokenUsage(customerId, conversationId,
                result.totalTokens, result.promptTokens, result.completionTokens,
                "gpt-4o-mini", "chat_completion");

        return toConversationResponse(conversation, customerId);
    }

    @Transactional
    public ConversationResponse addMessage(String customerId, String conversationId,
                                           String question, String customerType) {
        if (tokenService.isTokenQuotaExceeded(customerId)) {
            throw new IllegalStateException("今日Token配额已耗尽");
        }

        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在"));

        if (!customerId.equals(conversation.getCustomerId())) {
            throw new SecurityException("无权访问该对话");
        }

        AIResponseResult result = generateAIResponse(question, conversation.getPlatform());

        ChatMessage message = ChatMessage.builder()
                .conversationId(conversationId)
                .question(question)
                .customerType(customerType)
                .intentRecognition(result.intentRecognition)
                .replyStrategy(result.replyStrategy)
                .recommendedScript(result.recommendedScript)
                .hookGuidance(result.hookGuidance)
                .successClose(result.successClose)
                .riskWarning(result.riskWarning)
                .riskSuggestion(result.riskSuggestion)
                .totalTokens(result.totalTokens)
                .promptTokens(result.promptTokens)
                .completionTokens(result.completionTokens)
                .build();
        chatMessageRepository.save(message);

        tokenService.recordTokenUsage(customerId, conversationId,
                result.totalTokens, result.promptTokens, result.completionTokens,
                "gpt-4o-mini", "chat_completion");

        return toConversationResponse(conversation, customerId);
    }

    private AIResponseResult generateAIResponse(String question, String platform) {
        CompletableFuture<AIResponseResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                return simulateAIResponse(question, platform);
            } catch (Exception e) {
                log.error("AI generation failed: {}", e.getMessage(), e);
                throw new RuntimeException("AI生成失败", e);
            }
        }, executorService);

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("AI generation timeout after {} seconds", timeoutSeconds);
            throw new RuntimeException("AI生成超时", e);
        }
    }

    private AIResponseResult simulateAIResponse(String question, String platform) {
        int promptTokens = (question.length() + 500) / 4;
        int completionTokens = 300 + (int) (Math.random() * 200);
        int totalTokens = promptTokens + completionTokens;

        return new AIResponseResult(
                String.format("客户意图：售前咨询\n关键词：%s", extractKeywords(question)),
                String.format("先明确告知客户需求，再根据平台规则给出专业建议，最后引导客户继续咨询或下单。"),
                String.format("您好！感谢您的咨询。针对您提出的\"%s\"问题，我们非常重视。在%s平台上，我们提供专业的服务支持。",
                        truncate(question, 30), platform),
                "可补充当前活动优惠和售后保障，增强客户下单信心。",
                "您可以放心咨询，如果后续有任何问题，我这边也会协助您处理。",
                "当前平台：" + platform,
                "建议使用更中性的描述，避免平台敏感词",
                totalTokens,
                promptTokens,
                completionTokens
        );
    }

    private String extractKeywords(String question) {
        if (question.length() < 10) {
            return question;
        }
        return question.substring(0, Math.min(question.length(), 20)) + "...";
    }

    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private ConversationResponse toConversationResponse(Conversation conversation, String customerId) {
        Integer usedToday = tokenService.getTodayUsedTokens(customerId);
        Integer dailyLimit = tokenService.getDailyLimit();

        TokenInfo tokenInfo = TokenInfo.builder()
                .currentChatUsage(conversation.getId() != null ?
                        chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getConversationId())
                                .stream().mapToInt(ChatMessage::getTotalTokens).sum() : 0)
                .totalLimit(dailyLimit)
                .usedToday(usedToday)
                .usagePercent(usedToday != null ? (usedToday.doubleValue() / dailyLimit) * 100 : 0.0)
                .build();

        return ConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .platform(conversation.getPlatform())
                .title(conversation.getTitle())
                .messages(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getConversationId())
                        .stream()
                        .map(this::toChatMessageResponse)
                        .toList())
                .tokenInfo(tokenInfo)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .question(message.getQuestion())
                .customerType(message.getCustomerType())
                .intentRecognition(message.getIntentRecognition())
                .replyStrategy(message.getReplyStrategy())
                .recommendedScript(message.getRecommendedScript())
                .hookGuidance(message.getHookGuidance())
                .successClose(message.getSuccessClose())
                .riskWarning(message.getRiskWarning())
                .riskSuggestion(message.getRiskSuggestion())
                .totalTokens(message.getTotalTokens())
                .promptTokens(message.getPromptTokens())
                .completionTokens(message.getCompletionTokens())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private static class AIResponseResult {
        final String intentRecognition;
        final String replyStrategy;
        final String recommendedScript;
        final String hookGuidance;
        final String successClose;
        final String riskWarning;
        final String riskSuggestion;
        final Integer totalTokens;
        final Integer promptTokens;
        final Integer completionTokens;

        AIResponseResult(String intentRecognition, String replyStrategy, String recommendedScript,
                         String hookGuidance, String successClose, String riskWarning,
                         String riskSuggestion, Integer totalTokens, Integer promptTokens,
                         Integer completionTokens) {
            this.intentRecognition = intentRecognition;
            this.replyStrategy = replyStrategy;
            this.recommendedScript = recommendedScript;
            this.hookGuidance = hookGuidance;
            this.successClose = successClose;
            this.riskWarning = riskWarning;
            this.riskSuggestion = riskSuggestion;
            this.totalTokens = totalTokens;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
        }
    }
}
