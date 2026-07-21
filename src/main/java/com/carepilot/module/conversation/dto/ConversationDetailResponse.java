package com.carepilot.module.conversation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ConversationDetailResponse {
    private Long id;
    private String platform;
    private String question;
    private String category;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StepVO> steps;
    private TokenInfo tokenInfo;

    @Data
    @Builder
    public static class StepVO {
        private Long stepId;
        private Integer stepOrder;
        private String stepType;
        private String title;
        private String label;
        private List<String> tags;
        private String section;
        private String body;
        private Object riskWarning; // JSON object or null
        private String status;
        private Boolean editable;
    }

    @Data
    @Builder
    public static class TokenInfo {
        private Integer currentChatUsage;
        private Integer totalLimit;
        private Integer usedToday;
        private Double usagePercent;
    }
}
