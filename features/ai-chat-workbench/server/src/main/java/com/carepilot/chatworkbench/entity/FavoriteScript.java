package com.carepilot.chatworkbench.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_script", indexes = {
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_script_hash", columnList = "script_hash"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteScript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "conversation_id", length = 64)
    private String conversationId;

    @Column(name = "platform", nullable = false, length = 32)
    private String platform;

    @Column(name = "customer_type", length = 32)
    private String customerType;

    @Column(name = "script_step", nullable = false, length = 32)
    private String scriptStep;

    @Column(name = "script_content", nullable = false, columnDefinition = "TEXT")
    private String scriptContent;

    @Column(name = "script_hash", nullable = false, length = 64)
    private String scriptHash;

    @Column(name = "question", columnDefinition = "TEXT")
    private String question;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}