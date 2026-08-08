package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.dto.response.AiDialogStepResponse;
import com.carepilot.chatworkbench.dto.response.ChatMessageResponse;
import com.carepilot.chatworkbench.dto.response.ConversationResponse;
import com.carepilot.chatworkbench.dto.response.TokenInfo;
import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import com.carepilot.chatworkbench.entity.Conversation;
import com.carepilot.chatworkbench.repository.AiDialogStepRecordRepository;
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
    private final AiDialogStepRecordRepository stepRecordRepository;
    private final AiDialogStepAssembler stepAssembler;
    private final TokenService tokenService;

    public ConversationResponse getConversationById(String customerId, String conversationId) {
        Conversation conversation = ownedConversation(customerId, conversationId);
        return toResponse(conversation, customerId);
    }

    public List<AiDialogStepResponse> getEffectiveSteps(String customerId, String conversationId) {
        Conversation conversation = ownedConversation(customerId, conversationId);
        return stepAssembler.toSteps(stepRecordRepository
                .findBySessionTaskIdAndIsEffectiveAndIsDeleteOrderByDialogRoundAscStepNoAsc(
                        conversation.getId(), (byte) 1, (byte) 0));
    }

    public List<ConversationResponse> getConversationList(String customerId) {
        return conversationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(conversation -> toResponse(conversation, customerId))
                .toList();
    }

    @Transactional
    public ConversationResponse archiveConversation(String customerId, String conversationId) {
        ownedConversation(customerId, conversationId);
        conversationRepository.archiveOwnedConversation(customerId, conversationId);
        return getConversationById(customerId, conversationId);
    }

    public List<ConversationResponse> getRecentConversations(String customerId, Integer limit) {
        return conversationRepository.findRecentByCustomerId(customerId, limit).stream()
                .map(conversation -> toResponse(conversation, customerId))
                .toList();
    }

    @Transactional
    public void deleteConversation(String customerId, String conversationId) {
        Conversation conversation = ownedConversation(customerId, conversationId);
        stepRecordRepository.deleteBySessionTaskId(conversation.getId());
        conversationRepository.delete(conversation);
        log.info("Conversation deleted: customerId={}, conversationId={}", customerId, conversationId);
    }

    private Conversation ownedConversation(String customerId, String conversationId) {
        Conversation conversation = conversationRepository.findByConversationId(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Conversation does not exist"));
        if (!customerId.equals(conversation.getCustomerId())) {
            throw new SecurityException("Access to this conversation is forbidden");
        }
        return conversation;
    }

    private ConversationResponse toResponse(Conversation conversation, String customerId) {
        Integer usedToday = tokenService.getTodayUsedTokens(customerId);
        Integer dailyLimit = tokenService.getDailyLimit();
        List<AiDialogStepRecord> records = stepRecordRepository
                .findBySessionTaskIdAndIsEffectiveAndIsDeleteOrderByDialogRoundAscStepNoAsc(
                        conversation.getId(), (byte) 1, (byte) 0);
        List<ChatMessageResponse> messages = stepAssembler.toMessages(records);

        TokenInfo tokenInfo = TokenInfo.builder()
                .currentChatUsage(tokenService.getSessionUsedTokens(conversation.getId()))
                .totalLimit(dailyLimit)
                .usedToday(usedToday)
                .usagePercent(dailyLimit != null && dailyLimit > 0
                        ? (usedToday.doubleValue() / dailyLimit) * 100
                        : 0.0)
                .build();

        return ConversationResponse.builder()
                .conversationId(conversation.getConversationId())
                .sessionTaskId(conversation.getId())
                .platform(conversation.getPlatform())
                .title(conversation.getTitle())
                .status(conversation.getStatus())
                .messages(messages)
                .tokenInfo(tokenInfo)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
}
