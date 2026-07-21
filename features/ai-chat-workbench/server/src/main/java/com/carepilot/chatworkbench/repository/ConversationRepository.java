package com.carepilot.chatworkbench.repository;

import com.carepilot.chatworkbench.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByConversationId(String conversationId);

    List<Conversation> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    @Query("SELECT c FROM Conversation c WHERE c.customerId = :customerId ORDER BY c.createdAt DESC LIMIT :limit")
    List<Conversation> findRecentByCustomerId(@Param("customerId") String customerId, @Param("limit") Integer limit);
}