package com.carepilot.chatworkbench.repository;

import com.carepilot.chatworkbench.entity.CustomerServiceEvaluationReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CustomerServiceEvaluationReportRepository
        extends JpaRepository<CustomerServiceEvaluationReport, Long> {

    Optional<CustomerServiceEvaluationReport> findByConversationId(String conversationId);

    Optional<CustomerServiceEvaluationReport> findByIdAndOperatorId(Long id, String operatorId);

    @Query("""
            SELECT r FROM CustomerServiceEvaluationReport r
            WHERE r.operatorId = :operatorId
              AND (:fromAt IS NULL OR r.generatedAt >= :fromAt)
              AND (:toAt IS NULL OR r.generatedAt < :toAt)
              AND (:minScore IS NULL OR r.totalScore >= :minScore)
              AND (:maxScore IS NULL OR r.totalScore <= :maxScore)
              AND (:platform IS NULL OR :platform = '' OR r.platform = :platform)
            """)
    Page<CustomerServiceEvaluationReport> searchMine(
            @Param("operatorId") String operatorId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            @Param("minScore") Integer minScore,
            @Param("maxScore") Integer maxScore,
            @Param("platform") String platform,
            Pageable pageable);

    @Query("SELECT COUNT(r) FROM CustomerServiceEvaluationReport r WHERE r.operatorId = :operatorId")
    long countMine(@Param("operatorId") String operatorId);

    @Query("SELECT COALESCE(AVG(r.totalScore), 0) FROM CustomerServiceEvaluationReport r WHERE r.operatorId = :operatorId")
    Double averageMine(@Param("operatorId") String operatorId);
}
