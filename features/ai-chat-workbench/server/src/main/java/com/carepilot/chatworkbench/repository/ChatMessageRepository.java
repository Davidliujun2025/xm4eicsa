package com.carepilot.chatworkbench.repository;

import com.carepilot.chatworkbench.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId ORDER BY m.createdAt DESC LIMIT 1")
    ChatMessage findLatestByConversationId(@Param("conversationId") String conversationId);

    void deleteByConversationId(String conversationId);
}
