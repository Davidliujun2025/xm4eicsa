package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.entity.TokenUsageRecord;
import com.carepilot.chatworkbench.repository.TokenUsageRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenUsageRecordRepository tokenUsageRecordRepository;

    @Value("${app.token.daily-limit:50000}")
    private Integer dailyLimit;

    @Transactional
    public void recordTokenUsage(String customerId, String conversationId, Integer totalTokens,
                                 Integer promptTokens, Integer completionTokens,
                                 String modelName, String requestType) {
        TokenUsageRecord record = TokenUsageRecord.builder()
                .customerId(customerId)
                .conversationId(conversationId)
                .usageDate(LocalDate.now())
                .totalTokens(totalTokens)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .modelName(modelName)
                .requestType(requestType)
                .build();

        tokenUsageRecordRepository.save(record);
        log.info("Token usage recorded: customerId={}, conversationId={}, totalTokens={}",
                customerId, conversationId, totalTokens);
    }

    public Integer getTodayUsedTokens(String customerId) {
        return tokenUsageRecordRepository.sumTotalTokensByCustomerIdAndDate(customerId, LocalDate.now());
    }

    public Integer getTodayPromptTokens(String customerId) {
        return tokenUsageRecordRepository.sumPromptTokensByCustomerIdAndDate(customerId, LocalDate.now());
    }

    public Integer getTodayCompletionTokens(String customerId) {
        return tokenUsageRecordRepository.sumCompletionTokensByCustomerIdAndDate(customerId, LocalDate.now());
    }

    public boolean isTokenQuotaExceeded(String customerId) {
        Integer usedToday = getTodayUsedTokens(customerId);
        return usedToday != null && usedToday >= dailyLimit;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public Double getUsagePercent(String customerId) {
        Integer usedToday = getTodayUsedTokens(customerId);
        return usedToday != null ? (usedToday.doubleValue() / dailyLimit) * 100 : 0.0;
    }
}