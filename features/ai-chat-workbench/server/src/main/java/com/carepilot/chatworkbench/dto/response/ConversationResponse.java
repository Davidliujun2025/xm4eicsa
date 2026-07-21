package com.carepilot.chatworkbench.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private String conversationId;
    private String platform;
    private String title;
    private List<ChatMessageResponse> messages;
    private TokenInfo tokenInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
