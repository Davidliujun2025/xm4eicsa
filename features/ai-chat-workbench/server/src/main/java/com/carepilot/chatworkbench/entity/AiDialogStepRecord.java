package com.carepilot.chatworkbench.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_dialog_step_record",
        indexes = {
                @Index(name = "idx_session_task", columnList = "session_task_id"),
                @Index(name = "idx_task_effective", columnList = "session_task_id,dialog_round,is_effective,is_delete,step_no"),
                @Index(name = "idx_operator_trigger", columnList = "operator_id,trigger_at")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_task_dialog_step_round",
                columnNames = {"session_task_id", "dialog_round", "step_no", "step_round"}
        ))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiDialogStepRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "session_task_id", nullable = false)
    private Long sessionTaskId;

    @Column(name = "platform_id")
    private Long platformId;

    @Column(name = "customer_dialog", nullable = false, columnDefinition = "TEXT")
    private String customerDialog;

    @Column(name = "dialog_round", nullable = false)
    @Builder.Default
    private Integer dialogRound = 1;

    @Column(name = "step_no", nullable = false)
    private Byte stepNo;

    @Column(name = "step_round", nullable = false)
    @Builder.Default
    private Integer stepRound = 1;

    @Column(name = "ai_content", columnDefinition = "TEXT")
    private String aiContent;

    @Column(name = "extra_json", columnDefinition = "JSON")
    private String extraJson;

    @Column(name = "is_manual_edit", nullable = false)
    @Builder.Default
    private Byte isManualEdit = 0;

    @Column(name = "is_effective", nullable = false)
    @Builder.Default
    private Byte isEffective = 0;

    @Column(name = "trigger_at")
    private LocalDateTime triggerAt;

    @Column(name = "llm_reply_at")
    private LocalDateTime llmReplyAt;

    @Column(name = "generate_status", nullable = false)
    @Builder.Default
    private Byte generateStatus = 0;

    @Column(name = "fail_msg", length = 255)
    private String failMsg;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(name = "is_delete", nullable = false)
    @Builder.Default
    private Byte isDelete = 0;

    @Column(name = "prompt_tokens", nullable = false)
    @Builder.Default
    private Integer promptTokens = 0;

    @Column(name = "completion_tokens", nullable = false)
    @Builder.Default
    private Integer completionTokens = 0;

    @Column(name = "total_tokens", nullable = false)
    @Builder.Default
    private Integer totalTokens = 0;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;
}
