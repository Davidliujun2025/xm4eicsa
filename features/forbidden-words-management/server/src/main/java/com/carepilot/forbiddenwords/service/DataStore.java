package com.carepilot.forbiddenwords.service;

import com.carepilot.forbiddenwords.model.ForbiddenWord;
import com.carepilot.forbiddenwords.model.HitAuditLog;
import com.carepilot.forbiddenwords.model.OperationAuditLog;
import com.carepilot.forbiddenwords.model.Platform;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DataStore {
    List<ForbiddenWord> listWords();

    ForbiddenWord addWord(String word, Platform platform, String createdBy, LocalDateTime createdAt);

    Optional<ForbiddenWord> removeWordById(Long id);

    List<OperationAuditLog> listOperationLogs();

    OperationAuditLog addOperationLog(String action, String operator, String operatorIp, String targetWord, Platform platform, LocalDateTime operationTime);

    List<HitAuditLog> listHitLogs();

    HitAuditLog addHitLog(String actor, Platform platform, String sourceType, String content, String hitWord, String action, LocalDateTime actionTime);

    long countActorHitsOnDate(String actor, LocalDateTime dateTime);
}