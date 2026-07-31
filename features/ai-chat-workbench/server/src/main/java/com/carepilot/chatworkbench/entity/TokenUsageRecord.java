package com.carepilot.chatworkbench.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "token_usage_record", indexes = {
        @Index(name = "idx_token_usage_customer_id", columnList = "customer_id"),
        @Index(name = "idx_token_usage_date", columnList = "usage_date"),
        @Index(name = "idx_token_usage_created_at", columnList = "created_at"),
        @Index(name = "idx_token_usage_conversation_id", columnList = "conversation_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens;

    @Column(name = "prompt_tokens", nullable = false)
    private Integer promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private Integer completionTokens;

    @Column(name = "model_name", length = 64)
    private String modelName;

    @Column(name = "request_type", length = 32)
    private String requestType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
