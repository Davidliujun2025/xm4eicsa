package com.carepilot.chatworkbench.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_service_evaluation_report", indexes = {
        @Index(name = "idx_evaluation_operator_time", columnList = "operator_id,generated_at"),
        @Index(name = "idx_evaluation_operator_score", columnList = "operator_id,total_score"),
        @Index(name = "idx_evaluation_operator_platform", columnList = "operator_id,platform")
}, uniqueConstraints = @UniqueConstraint(name = "uk_evaluation_conversation", columnNames = "conversation_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerServiceEvaluationReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "conversation_id", nullable = false, length = 64)
    private String conversationId;
    @Column(name = "session_task_id", nullable = false)
    private Long sessionTaskId;
    @Column(name = "operator_id", nullable = false, length = 64)
    private String operatorId;
    @Column(nullable = false, length = 32)
    private String platform;
    @Column(name = "total_score", nullable = false)
    private Integer totalScore;
    @Column(name = "service_attitude_score", nullable = false)
    private Integer serviceAttitudeScore;
    @Column(name = "problem_solving_score", nullable = false)
    private Integer problemSolvingScore;
    @Column(name = "empathy_score", nullable = false)
    private Integer empathyScore;
    @Column(name = "compliance_score", nullable = false)
    private Integer complianceScore;
    @Column(name = "conversion_guidance_score", nullable = false)
    private Integer conversionGuidanceScore;
    @Column(name = "response_efficiency_score", nullable = false)
    private Integer responseEfficiencyScore;
    @Column(name = "dimension_weights_json", nullable = false, columnDefinition = "JSON")
    private String dimensionWeightsJson;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String strengths;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String problems;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String suggestions;
    @Column(name = "forbidden_hit_count", nullable = false)
    private Integer forbiddenHitCount;
    @Column(name = "forbidden_words_json", nullable = false, columnDefinition = "JSON")
    private String forbiddenWordsJson;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String transcript;
    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens;
    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;
}
