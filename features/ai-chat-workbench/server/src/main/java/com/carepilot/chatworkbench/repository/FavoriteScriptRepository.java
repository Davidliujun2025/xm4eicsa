package com.carepilot.chatworkbench.repository;

import com.carepilot.chatworkbench.entity.FavoriteScript;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteScriptRepository extends JpaRepository<FavoriteScript, Long> {

    Optional<FavoriteScript> findByCustomerIdAndScriptHash(String customerId, String scriptHash);

    List<FavoriteScript> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<FavoriteScript> findByCustomerIdAndPlatformOrderByCreatedAtDesc(String customerId, String platform);

    boolean existsByCustomerIdAndScriptHash(String customerId, String scriptHash);

    void deleteByCustomerIdAndScriptHash(String customerId, String scriptHash);

    List<FavoriteScript> findByConversationId(String conversationId);
}