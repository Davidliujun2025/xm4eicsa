package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class ConversationArchiveScheduler {

    private final ConversationRepository conversationRepository;

    @Scheduled(
            fixedDelayString = "${app.conversation.archive-interval-ms:60000}",
            initialDelayString = "${app.conversation.archive-initial-delay-ms:10000}")
    @Transactional
    public void archiveInactiveConversations() {
        int archived = conversationRepository.archiveInactiveConversations();
        if (archived > 0) {
            log.info("Archived inactive conversations: count={}", archived);
        }
    }
}
