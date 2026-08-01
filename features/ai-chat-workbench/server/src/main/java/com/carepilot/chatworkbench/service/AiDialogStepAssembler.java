package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.dto.response.AiDialogStepResponse;
import com.carepilot.chatworkbench.dto.response.ChatMessageResponse;
import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
public class AiDialogStepAssembler {

    private static final TypeReference<Map<String, Object>> EXTRA_TYPE = new TypeReference<>() { };

    private final ObjectMapper objectMapper;

    public List<ChatMessageResponse> toMessages(List<AiDialogStepRecord> records) {
        Map<Integer, List<AiDialogStepRecord>> byDialogRound = new TreeMap<>();
        for (AiDialogStepRecord record : records) {
            byDialogRound.computeIfAbsent(record.getDialogRound(), ignored -> new ArrayList<>()).add(record);
        }

        return byDialogRound.entrySet().stream()
                .map(entry -> toMessage(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<AiDialogStepResponse> toSteps(List<AiDialogStepRecord> records) {
        return records.stream().map(this::toStep).toList();
    }

    public String createExtraJson(String customerType, String riskWarning, String riskSuggestion) {
        Map<String, Object> extra = new LinkedHashMap<>();
        putIfPresent(extra, "customer_type", customerType);
        putIfPresent(extra, "risk_warning", riskWarning);
        putIfPresent(extra, "risk_suggestion", riskSuggestion);
        try {
            return objectMapper.writeValueAsString(extra);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to serialize AI dialog metadata", exception);
        }
    }

    private ChatMessageResponse toMessage(Integer dialogRound, List<AiDialogStepRecord> records) {
        AiDialogStepRecord first = records.getFirst();
        Map<String, Object> extra = readExtra(records);
        LocalDateTime createdAt = records.stream()
                .map(AiDialogStepRecord::getCreateTime)
                .filter(value -> value != null)
                .min(LocalDateTime::compareTo)
                .orElse(first.getTriggerAt());

        return ChatMessageResponse.builder()
                .id(first.getRecordId())
                .dialogRound(dialogRound)
                .question(first.getCustomerDialog())
                .customerType(asString(extra.get("customer_type")))
                .intentRecognition(contentForStep(records, 1))
                .replyStrategy(contentForStep(records, 2))
                .recommendedScript(contentForStep(records, 3))
                .hookGuidance(contentForStep(records, 4))
                .successClose(contentForStep(records, 5))
                .riskWarning(asString(extra.get("risk_warning")))
                .riskSuggestion(asString(extra.get("risk_suggestion")))
                .promptTokens(sum(records, AiDialogStepRecord::getPromptTokens))
                .completionTokens(sum(records, AiDialogStepRecord::getCompletionTokens))
                .totalTokens(sum(records, AiDialogStepRecord::getTotalTokens))
                .createdAt(createdAt)
                .build();
    }

    private AiDialogStepResponse toStep(AiDialogStepRecord record) {
        return AiDialogStepResponse.builder()
                .recordId(record.getRecordId())
                .sessionTaskId(record.getSessionTaskId())
                .platformId(record.getPlatformId())
                .customerDialog(record.getCustomerDialog())
                .dialogRound(record.getDialogRound())
                .stepNo(record.getStepNo())
                .stepRound(record.getStepRound())
                .aiContent(record.getAiContent())
                .extraJson(record.getExtraJson())
                .isManualEdit(record.getIsManualEdit())
                .isEffective(record.getIsEffective())
                .triggerAt(record.getTriggerAt())
                .llmReplyAt(record.getLlmReplyAt())
                .generateStatus(record.getGenerateStatus())
                .failMsg(record.getFailMsg())
                .operatorId(record.getOperatorId())
                .traceId(record.getTraceId())
                .isDelete(record.getIsDelete())
                .promptTokens(record.getPromptTokens())
                .completionTokens(record.getCompletionTokens())
                .totalTokens(record.getTotalTokens())
                .createTime(record.getCreateTime())
                .updateTime(record.getUpdateTime())
                .build();
    }

    private Map<String, Object> readExtra(List<AiDialogStepRecord> records) {
        for (AiDialogStepRecord record : records) {
            if (record.getExtraJson() == null || record.getExtraJson().isBlank()) {
                continue;
            }
            try {
                return objectMapper.readValue(record.getExtraJson(), EXTRA_TYPE);
            } catch (Exception ignored) {
                // Ignore malformed legacy metadata; the generated content remains readable.
            }
        }
        return Map.of();
    }

    private String contentForStep(List<AiDialogStepRecord> records, int stepNo) {
        return records.stream()
                .filter(record -> record.getStepNo() != null && record.getStepNo().intValue() == stepNo)
                .map(AiDialogStepRecord::getAiContent)
                .findFirst()
                .orElse(null);
    }

    private Integer sum(List<AiDialogStepRecord> records,
                        java.util.function.Function<AiDialogStepRecord, Integer> getter) {
        return records.stream().map(getter).filter(value -> value != null).mapToInt(Integer::intValue).sum();
    }

    private void putIfPresent(Map<String, Object> target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }
}
