package com.carepilot.chatworkbench.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AiDialogStepResponse(
        Long recordId,
        Long sessionTaskId,
        Long platformId,
        String customerDialog,
        Integer dialogRound,
        Byte stepNo,
        Integer stepRound,
        String aiContent,
        String extraJson,
        Byte isManualEdit,
        Byte isEffective,
        LocalDateTime triggerAt,
        LocalDateTime llmReplyAt,
        Byte generateStatus,
        String failMsg,
        Long operatorId,
        String traceId,
        Byte isDelete,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
