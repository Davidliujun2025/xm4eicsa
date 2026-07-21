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
public class FavoriteScriptResponse {

    private Long id;
    private String conversationId;
    private String platform;
    private String customerType;
    private String scriptStep;
    private String scriptContent;
    private String question;
    private LocalDateTime createdAt;
}