package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.dto.response.ChatMessageResponse;
import com.carepilot.chatworkbench.dto.response.ConversationResponse;
import com.carepilot.chatworkbench.dto.response.TokenInfo;
import com.carepilot.chatworkbench.entity.ChatMessage;
import com.carepilot.chatworkbench.entity.Conversation;
import com.carepilot.chatworkbench.repository.ChatMessageRepository;
import com.carepilot.chatworkbench.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TokenService tokenService;

    public ConversationResponse getConversationById(String customerId, String conversationId) {
        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在"));

        if (!customerId.equals(conversation.getCustomerId())) {
            throw new SecurityException("无权访问该对话");
        }

        return toResponse(conversation, customerId);
    }

    public List<ConversationResponse> getConversationList(String customerId) {
        List<Conversation> conversations = conversationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return conversations.stream()
                .map(c -> toResponse(c, customerId))
                .toList();
    }

    public List<ConversationResponse> getRecentConversations(String customerId, Integer limit) {
        List<Conversation> conversations = conversationRepository.findRecentByCustomerId(customerId, limit);
        return conversations.stream()
                .map(c -> toResponse(c, customerId))
                .toList();
    }

    @Transactional
    public void deleteConversation(String customerId, String conversationId) {
        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("对话不存在"));

        if (!customerId.equals(conversation.getCustomerId())) {
            throw new SecurityException("无权删除该对话");
        }

        chatMessageRepository.deleteByConversationId(conversationId);
        conversationRepository.delete(conversation);
        log.info("Conversation deleted: customerId={}, conversationId={}", customerId, conversationId);
    }

    private ConversationResponse toResponse(Conversation conversation, String customerId) {
        Integer usedToday = tokenService.getTodayUsedTokens(customerId);
        Integer dailyLimit = tokenService.getDailyLimit();

        List<ChatMessage> messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getConversationId());

        TokenInfo tokenInfo = TokenInfo.builder()
                .currentChatUsage(messages.stream().mapToInt(ChatMessage::getTotalTokens).sum())
                .totalLimit(dailyLimit)
                .usedToday(usedToday)
                .usagePercent(usedToday != null ? (usedToday.doubleValue() / dailyLimit) * 100 : 0.0)
                .build();

        return ConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .platform(conversation.getPlatform())
                .title(conversation.getTitle())
                .messages(messages.stream().map(this::toChatMessageResponse).toList())
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
}
