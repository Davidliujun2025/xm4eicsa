package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import com.carepilot.chatworkbench.enums.ScriptStepEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DialogContextBuilder {

    public String buildConversationContext(List<AiDialogStepRecord> historyRecords) {
        List<AiDialogStepRecord> sortedRecords = sortedRecords(historyRecords);
        Map<Integer, String> questionHistory = new LinkedHashMap<>();
        Map<Integer, String> intentHistory = new LinkedHashMap<>();

        for (AiDialogStepRecord record : sortedRecords) {
            Integer dialogRound = record.getDialogRound();
            if (dialogRound == null || dialogRound <= 0) {
                continue;
            }
            if (record.getCustomerDialog() != null && !record.getCustomerDialog().isBlank()) {
                questionHistory.putIfAbsent(dialogRound, record.getCustomerDialog());
            }
            if (Byte.valueOf((byte) 1).equals(record.getStepNo())
                    && record.getAiContent() != null && !record.getAiContent().isBlank()) {
                intentHistory.put(dialogRound, record.getAiContent());
            }
        }

        return formatContext(questionHistory, intentHistory, "意图识别");
    }

    /**
     * Builds the database value for ai_dialog_step_record.dialog_context.
     * This context deliberately excludes prompts, keywords and other model-only instructions.
     */
    public String buildStoredStepContext(List<AiDialogStepRecord> historyRecords,
                                         String currentQuestion,
                                         int currentDialogRound,
                                         int currentStepNo,
                                         int currentStepRound,
                                         String currentAiContent) {
        ScriptStepEnum[] steps = ScriptStepEnum.values();
        if (currentStepNo < 1 || currentStepNo > steps.length) {
            throw new IllegalArgumentException("Current step number is outside configured steps");
        }

        List<AiDialogStepRecord> sortedRecords = sortedRecords(historyRecords);
        Map<Integer, String> questionHistory = new LinkedHashMap<>();
        List<AiDialogStepRecord> sameStepHistory = new ArrayList<>();

        for (AiDialogStepRecord record : sortedRecords) {
            Integer dialogRound = record.getDialogRound();
            if (dialogRound == null || dialogRound <= 0) {
                continue;
            }
            if (record.getCustomerDialog() != null && !record.getCustomerDialog().isBlank()) {
                questionHistory.putIfAbsent(dialogRound, record.getCustomerDialog());
            }
            if (record.getStepNo() != null
                    && record.getStepNo().intValue() == currentStepNo
                    && record.getAiContent() != null
                    && !record.getAiContent().isBlank()) {
                sameStepHistory.add(record);
            }
        }

        if (currentQuestion != null && !currentQuestion.isBlank()) {
            questionHistory.put(currentDialogRound, currentQuestion);
        }

        StringBuilder builder = new StringBuilder("用户问题历史：");
        appendHistory(builder, questionHistory);
        builder.append("\n\n")
                .append(steps[currentStepNo - 1].getDescription())
                .append("历史：");
        for (AiDialogStepRecord record : sameStepHistory) {
            appendStepOutput(builder, record.getDialogRound(), record.getStepRound(), record.getAiContent());
        }
        if (currentAiContent != null && !currentAiContent.isBlank()) {
            appendStepOutput(
                    builder, currentDialogRound, currentStepRound, currentAiContent);
        }
        if (sameStepHistory.isEmpty()
                && (currentAiContent == null || currentAiContent.isBlank())) {
            builder.append("\n无");
        }
        return builder.toString();
    }

    public List<String> buildStepContexts(List<AiDialogStepRecord> historyRecords,
                                          String currentQuestion,
                                          List<String> currentStepContents) {
        ScriptStepEnum[] steps = ScriptStepEnum.values();
        if (currentStepContents.size() != steps.length) {
            throw new IllegalArgumentException("Current step content size does not match configured steps");
        }

        List<AiDialogStepRecord> sortedRecords = sortedRecords(historyRecords);

        Map<Integer, String> questionHistory = new LinkedHashMap<>();
        Map<Integer, Map<Integer, String>> stepHistory = new LinkedHashMap<>();
        int maxDialogRound = 0;

        for (AiDialogStepRecord record : sortedRecords) {
            Integer dialogRound = record.getDialogRound();
            if (dialogRound == null || dialogRound <= 0) {
                continue;
            }
            maxDialogRound = Math.max(maxDialogRound, dialogRound);

            if (record.getCustomerDialog() != null && !record.getCustomerDialog().isBlank()) {
                questionHistory.putIfAbsent(dialogRound, record.getCustomerDialog());
            }

            Byte stepNo = record.getStepNo();
            if (stepNo == null || stepNo <= 0 || stepNo > steps.length
                    || record.getAiContent() == null || record.getAiContent().isBlank()) {
                continue;
            }

            stepHistory.computeIfAbsent((int) stepNo, ignored -> new LinkedHashMap<>())
                    .put(dialogRound, record.getAiContent());
        }

        int currentDialogRound = maxDialogRound + 1;
        if (currentQuestion != null && !currentQuestion.isBlank()) {
            questionHistory.put(currentDialogRound, currentQuestion);
        }

        List<String> contexts = new ArrayList<>(steps.length);
        for (int index = 0; index < steps.length; index++) {
            int stepNo = index + 1;
            Map<Integer, String> currentStepHistory = new LinkedHashMap<>(
                    stepHistory.getOrDefault(stepNo, Map.of()));
            String currentContent = currentStepContents.get(index);
            if (currentContent != null && !currentContent.isBlank()) {
                currentStepHistory.put(currentDialogRound, currentContent);
            }
            contexts.add(formatContext(questionHistory, currentStepHistory, steps[index].getDescription()));
        }

        return contexts;
    }

    private List<AiDialogStepRecord> sortedRecords(List<AiDialogStepRecord> historyRecords) {
        return historyRecords.stream()
                .sorted(Comparator
                        .comparing(AiDialogStepRecord::getDialogRound, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(AiDialogStepRecord::getStepNo, Comparator.nullsLast(Byte::compareTo))
                        .thenComparing(AiDialogStepRecord::getStepRound, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private String formatContext(Map<Integer, String> questionHistory,
                                 Map<Integer, String> stepHistory,
                                 String stepDescription) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户问题历史：");
        appendHistory(builder, questionHistory);
        builder.append("\n\n").append(stepDescription).append("历史：");
        appendHistory(builder, stepHistory);
        return builder.toString();
    }

    private void appendHistory(StringBuilder builder, Map<Integer, String> history) {
        if (history.isEmpty()) {
            builder.append("\n无");
            return;
        }
        history.forEach((dialogRound, content) ->
                builder.append("\n第").append(dialogRound).append("轮：").append(content));
    }

    private void appendStepOutput(StringBuilder builder, Integer dialogRound,
                                  Integer stepRound, String content) {
        builder.append("\n第").append(dialogRound).append("轮");
        if (stepRound != null && stepRound > 1) {
            builder.append("（第").append(stepRound).append("次生成）");
        }
        builder.append("：").append(content);
    }
}
