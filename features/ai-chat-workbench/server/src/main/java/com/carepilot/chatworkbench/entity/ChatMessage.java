package com.carepilot.chatworkbench.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_message", indexes = {
        @Index(name = "idx_chat_conversation_id", columnList = "conversation_id"),
        @Index(name = "idx_chat_message_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "customer_type", length = 32)
    private String customerType;

    @Column(name = "intent_recognition", columnDefinition = "TEXT")
    private String intentRecognition;

    @Column(name = "reply_strategy", columnDefinition = "TEXT")
    private String replyStrategy;

    @Column(name = "recommended_script", columnDefinition = "TEXT")
    private String recommendedScript;

    @Column(name = "hook_guidance", columnDefinition = "TEXT")
    private String hookGuidance;

    @Column(name = "success_close", columnDefinition = "TEXT")
    private String successClose;

    @Column(name = "risk_warning", length = 512)
    private String riskWarning;

    @Column(name = "risk_suggestion", length = 512)
    private String riskSuggestion;

    @Column(name = "total_tokens", nullable = false)
    @Builder.Default
    private Integer totalTokens = 0;

    @Column(name = "prompt_tokens", nullable = false)
    @Builder.Default
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens", nullable = false)
    @Builder.Default
    private Integer completionTokens = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
