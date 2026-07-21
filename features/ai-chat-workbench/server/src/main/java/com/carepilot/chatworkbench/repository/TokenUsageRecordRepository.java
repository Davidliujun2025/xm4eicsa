package com.carepilot.chatworkbench.repository;

import com.carepilot.chatworkbench.entity.TokenUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TokenUsageRecordRepository extends JpaRepository<TokenUsageRecord, Long> {

    @Query("SELECT COALESCE(SUM(t.totalTokens), 0) FROM TokenUsageRecord t WHERE t.customerId = :customerId AND t.usageDate = :date")
    Integer sumTotalTokensByCustomerIdAndDate(@Param("customerId") String customerId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(t.promptTokens), 0) FROM TokenUsageRecord t WHERE t.customerId = :customerId AND t.usageDate = :date")
    Integer sumPromptTokensByCustomerIdAndDate(@Param("customerId") String customerId, @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(t.completionTokens), 0) FROM TokenUsageRecord t WHERE t.customerId = :customerId AND t.usageDate = :date")
    Integer sumCompletionTokensByCustomerIdAndDate(@Param("customerId") String customerId, @Param("date") LocalDate date);

    List<TokenUsageRecord> findByCustomerIdAndUsageDateOrderByCreatedAtDesc(String customerId, LocalDate date);

    List<TokenUsageRecord> findByConversationId(String conversationId);

    @Query("SELECT t FROM TokenUsageRecord t WHERE t.customerId = :customerId ORDER BY t.createdAt DESC LIMIT :limit")
    List<TokenUsageRecord> findRecentByCustomerId(@Param("customerId") String customerId, @Param("limit") Integer limit);
}