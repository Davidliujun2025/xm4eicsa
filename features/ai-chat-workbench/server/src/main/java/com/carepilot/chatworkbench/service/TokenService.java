package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.repository.AiDialogStepRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final AiDialogStepRecordRepository stepRecordRepository;

    @Value("${app.token.daily-limit:50000}")
    private Integer dailyLimit;

    public Integer getTodayUsedTokens(String customerId) {
        LocalDateTime startAt = LocalDate.now().atStartOfDay();
        return toInteger(stepRecordRepository.sumTotalTokens(operatorId(customerId), startAt, startAt.plusDays(1)));
    }

    public Integer getTodayPromptTokens(String customerId) {
        LocalDateTime startAt = LocalDate.now().atStartOfDay();
        return toInteger(stepRecordRepository.sumPromptTokens(operatorId(customerId), startAt, startAt.plusDays(1)));
    }

    public Integer getTodayCompletionTokens(String customerId) {
        LocalDateTime startAt = LocalDate.now().atStartOfDay();
        return toInteger(stepRecordRepository.sumCompletionTokens(operatorId(customerId), startAt, startAt.plusDays(1)));
    }

    public Integer getSessionUsedTokens(Long sessionTaskId) {
        return toInteger(stepRecordRepository.sumTotalTokensBySessionTaskId(sessionTaskId));
    }

    public boolean isTokenQuotaExceeded(String customerId) {
        return getTodayUsedTokens(customerId) >= dailyLimit;
    }

    public Integer getDailyLimit() {
        return dailyLimit;
    }

    public Double getUsagePercent(String customerId) {
        if (dailyLimit == null || dailyLimit <= 0) {
            return 0.0;
        }
        return (getTodayUsedTokens(customerId).doubleValue() / dailyLimit) * 100;
    }

    private Long operatorId(String customerId) {
        try {
            return Long.valueOf(customerId);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid operator id: " + customerId, exception);
        }
    }

    private Integer toInteger(Long value) {
        if (value == null) {
            return 0;
        }
        return Math.toIntExact(value);
    }
}
