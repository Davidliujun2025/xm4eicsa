package com.carepilot.forbiddenwords.service;

import com.carepilot.forbiddenwords.model.ForbiddenWord;
import com.carepilot.forbiddenwords.model.Platform;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class ForbiddenWordCacheService {
    private final AtomicReference<List<ForbiddenWord>> aiCache = new AtomicReference<>(new ArrayList<>());
    private final int syncWindowMinutes;
    private volatile LocalDateTime lastRefreshedAt;

    public ForbiddenWordCacheService(@Value("${app.cache.sync-window-minutes:5}") int syncWindowMinutes) {
        this.syncWindowMinutes = syncWindowMinutes;
        this.lastRefreshedAt = LocalDateTime.now();
    }

    public void refresh(List<ForbiddenWord> latestWords) {
        aiCache.set(new ArrayList<>(latestWords));
        lastRefreshedAt = LocalDateTime.now();
    }

    public List<String> findHitWords(Platform platform, String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String normalized = content.toLowerCase();
        return aiCache.get().stream()
                .filter(w -> w.getPlatform() == Platform.ALL || w.getPlatform() == platform)
                .map(ForbiddenWord::getWord)
                .filter(word -> normalized.contains(word.toLowerCase()))
                .distinct()
                .collect(Collectors.toList());
    }

    public int getSyncWindowMinutes() {
        return syncWindowMinutes;
    }

    public LocalDateTime getLastRefreshedAt() {
        return lastRefreshedAt;
    }
}
