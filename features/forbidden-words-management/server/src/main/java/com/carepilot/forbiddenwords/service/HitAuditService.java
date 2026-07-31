package com.carepilot.forbiddenwords.service;

import com.carepilot.forbiddenwords.dto.ChatAuditRequest;
import com.carepilot.forbiddenwords.dto.PageResult;
import com.carepilot.forbiddenwords.model.HitAuditLog;
import com.carepilot.forbiddenwords.model.Platform;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HitAuditService {
    private final DataStore store;
    private final ForbiddenWordCacheService cacheService;

    public HitAuditService(DataStore store, ForbiddenWordCacheService cacheService) {
        this.store = store;
        this.cacheService = cacheService;
    }

    public Map<String, Object> auditChat(ChatAuditRequest req) {
        List<String> questionHits = cacheService.findHitWords(req.getPlatform(), req.getUserQuestion());
        List<String> answerHits = cacheService.findHitWords(req.getPlatform(), req.getAiAnswer());

        for (String hit : questionHits) {
            store.addHitLog(
                    req.getActor(),
                    req.getPlatform(),
                    "USER_QUESTION",
                    req.getUserQuestion(),
                    hit,
                    "WARN_AGENT",
                    LocalDateTime.now()
            );
        }

        for (String hit : answerHits) {
            store.addHitLog(
                    req.getActor(),
                    req.getPlatform(),
                    "AI_ANSWER",
                    req.getAiAnswer(),
                    hit,
                    "BLOCK_REPLY",
                    LocalDateTime.now()
            );
        }

        long todayCount = store.countActorHitsOnDate(req.getActor(), LocalDateTime.now());

        Map<String, Object> response = new HashMap<>();
        response.put("questionHits", questionHits);
        response.put("answerHits", answerHits);
        response.put("todayTriggerCount", todayCount);
        response.put("shouldRemindAgent", todayCount >= 3);
        return response;
    }

    public PageResult<HitAuditLog> listHitLogs(String actor, Platform platform, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 20 : size;

        List<HitAuditLog> filtered = store.listHitLogs().stream()
                .filter(log -> actor == null || actor.isBlank() || log.getActor().toLowerCase().contains(actor.toLowerCase()))
                .filter(log -> platform == null || platform == Platform.ALL || log.getPlatform() == platform)
                .sorted(Comparator.comparing(HitAuditLog::getActionTime).reversed())
                .collect(Collectors.toList());

        int from = (safePage - 1) * safeSize;
        if (from >= filtered.size()) {
            return new PageResult<>(List.of(), filtered.size(), safePage, safeSize);
        }
        int to = Math.min(from + safeSize, filtered.size());
        return new PageResult<>(filtered.subList(from, to), filtered.size(), safePage, safeSize);
    }

    public PageResult<HitAuditLog> listCurrentAgentHitLogs(Set<String> actorKeys, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);

        Set<String> normalizedActorKeys = actorKeys.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<HitAuditLog> filtered = store.listHitLogs().stream()
                .filter(log -> log.getActor() != null
                        && normalizedActorKeys.contains(log.getActor().trim().toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(HitAuditLog::getActionTime).reversed())
                .collect(Collectors.toList());

        int from = (safePage - 1) * safeSize;
        if (from >= filtered.size()) {
            return new PageResult<>(List.of(), filtered.size(), safePage, safeSize);
        }
        int to = Math.min(from + safeSize, filtered.size());
        return new PageResult<>(filtered.subList(from, to), filtered.size(), safePage, safeSize);
    }
}
