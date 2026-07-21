package com.carepilot.chatworkbench.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private String question;
    private String customerType;
    private String intentRecognition;
    private String replyStrategy;
    private String recommendedScript;
    private String hookGuidance;
    private String successClose;
    private String riskWarning;
    private String riskSuggestion;
    private Integer totalTokens;
    private Integer promptTokens;
    private Integer completionTokens;
    private LocalDateTime createdAt;
}
