package com.carepilot.forbiddenwords.service;

import com.carepilot.forbiddenwords.model.ForbiddenWord;
import com.carepilot.forbiddenwords.model.HitAuditLog;
import com.carepilot.forbiddenwords.model.OperationAuditLog;
import com.carepilot.forbiddenwords.model.Platform;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("local")
public class InMemoryStore implements DataStore {
    private final List<ForbiddenWord> words = new CopyOnWriteArrayList<>();
    private final List<OperationAuditLog> operationLogs = new CopyOnWriteArrayList<>();
    private final List<HitAuditLog> hitLogs = new CopyOnWriteArrayList<>();

    private final AtomicLong wordIdGenerator = new AtomicLong(1);
    private final AtomicLong operationLogIdGenerator = new AtomicLong(1);
    private final AtomicLong hitLogIdGenerator = new AtomicLong(1);

    @Override
    public List<ForbiddenWord> listWords() {
        return new ArrayList<>(words);
    }

    @Override
    public long countWords() {
        return words.size();
    }

    @Override
    public long countCoveredPlatforms() {
        if (words.stream().anyMatch(word -> word.getPlatform() == Platform.ALL)) {
            return java.util.Arrays.stream(Platform.values())
                    .filter(platform -> platform != Platform.ALL)
                    .count();
        }
        return words.stream()
                .map(ForbiddenWord::getPlatform)
                .filter(platform -> platform != Platform.ALL)
                .distinct()
                .count();
    }

    @Override
    public ForbiddenWord addWord(String word, Platform platform, String createdBy, LocalDateTime createdAt) {
        ForbiddenWord created = new ForbiddenWord(
                wordIdGenerator.getAndIncrement(),
                word,
                platform,
                createdAt,
                createdBy
        );
        words.add(created);
        return created;
    }

    @Override
    public Optional<ForbiddenWord> removeWordById(Long id) {
        Optional<ForbiddenWord> target = words.stream().filter(w -> Objects.equals(w.getId(), id)).findFirst();
        target.ifPresent(words::remove);
        return target;
    }

    @Override
    public List<OperationAuditLog> listOperationLogs() {
        return new ArrayList<>(operationLogs);
    }

    @Override
    public OperationAuditLog addOperationLog(String action, String operator, String operatorIp, String targetWord, Platform platform, LocalDateTime operationTime) {
        OperationAuditLog log = new OperationAuditLog(
                operationLogIdGenerator.getAndIncrement(),
                action,
                operator,
                operatorIp,
                targetWord,
                platform,
                operationTime
        );
        operationLogs.add(log);
        return log;
    }

    @Override
    public List<HitAuditLog> listHitLogs() {
        return new ArrayList<>(hitLogs);
    }

    @Override
    public HitAuditLog addHitLog(String actor, Platform platform, String sourceType, String content, String hitWord, String action, LocalDateTime actionTime) {
        HitAuditLog log = new HitAuditLog(
                hitLogIdGenerator.getAndIncrement(),
                actor,
                platform,
                sourceType,
                content,
                hitWord,
                action,
                actionTime
        );
        hitLogs.add(log);
        return log;
    }

    @Override
    public long countActorHitsOnDate(String actor, LocalDateTime dateTime) {
        return hitLogs.stream()
                .filter(log -> Objects.equals(log.getActor(), actor))
                .filter(log -> log.getActionTime().toLocalDate().equals(dateTime.toLocalDate()))
                .count();
    }

    @Override
    public long countHitsOnDate(LocalDateTime dateTime) {
        return hitLogs.stream()
                .filter(log -> log.getActionTime().toLocalDate().equals(dateTime.toLocalDate()))
                .count();
    }
}
