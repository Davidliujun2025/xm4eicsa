package com.carepilot.chatworkbench.entity;

import com.carepilot.chatworkbench.enums.ConversationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation", indexes = {
        @Index(name = "idx_conversation_customer_id", columnList = "customer_id"),
        @Index(name = "idx_conversation_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, unique = true, length = 64)
    private String conversationId;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "platform", nullable = false, length = 32)
    private String platform;

    @Column(name = "title", length = 255)
    private String title;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ConversationStatus status = ConversationStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
