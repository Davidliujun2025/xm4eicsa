package com.carepilot.chatworkbench.repository;

import com.carepilot.chatworkbench.entity.AiDialogStepRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiDialogStepRecordRepository extends JpaRepository<AiDialogStepRecord, Long> {

    List<AiDialogStepRecord> findBySessionTaskIdAndIsEffectiveAndIsDeleteOrderByDialogRoundAscStepNoAsc(
            Long sessionTaskId, Byte isEffective, Byte isDelete);

    List<AiDialogStepRecord> findBySessionTaskIdAndIsDeleteOrderByDialogRoundAscStepNoAscStepRoundAsc(
            Long sessionTaskId, Byte isDelete);

    @Query("SELECT COALESCE(MAX(r.dialogRound), 0) FROM AiDialogStepRecord r WHERE r.sessionTaskId = :sessionTaskId")
    Integer findMaxDialogRound(@Param("sessionTaskId") Long sessionTaskId);

    @Query("SELECT COALESCE(SUM(r.totalTokens), 0) FROM AiDialogStepRecord r "
            + "WHERE r.operatorId = :operatorId AND r.generateStatus = 1 AND r.isDelete = 0 "
            + "AND r.triggerAt >= :startAt AND r.triggerAt < :endAt")
    Long sumTotalTokens(@Param("operatorId") Long operatorId,
                        @Param("startAt") LocalDateTime startAt,
                        @Param("endAt") LocalDateTime endAt);

    @Query("SELECT COALESCE(SUM(r.promptTokens), 0) FROM AiDialogStepRecord r "
            + "WHERE r.operatorId = :operatorId AND r.generateStatus = 1 AND r.isDelete = 0 "
            + "AND r.triggerAt >= :startAt AND r.triggerAt < :endAt")
    Long sumPromptTokens(@Param("operatorId") Long operatorId,
                         @Param("startAt") LocalDateTime startAt,
                         @Param("endAt") LocalDateTime endAt);

    @Query("SELECT COALESCE(SUM(r.completionTokens), 0) FROM AiDialogStepRecord r "
            + "WHERE r.operatorId = :operatorId AND r.generateStatus = 1 AND r.isDelete = 0 "
            + "AND r.triggerAt >= :startAt AND r.triggerAt < :endAt")
    Long sumCompletionTokens(@Param("operatorId") Long operatorId,
                             @Param("startAt") LocalDateTime startAt,
                             @Param("endAt") LocalDateTime endAt);

    @Query("SELECT COALESCE(SUM(r.totalTokens), 0) FROM AiDialogStepRecord r "
            + "WHERE r.sessionTaskId = :sessionTaskId AND r.generateStatus = 1 AND r.isDelete = 0")
    Long sumTotalTokensBySessionTaskId(@Param("sessionTaskId") Long sessionTaskId);

    void deleteBySessionTaskId(Long sessionTaskId);
}
