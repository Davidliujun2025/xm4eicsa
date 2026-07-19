package com.carepilot.forbiddenwords.model;

import java.time.LocalDateTime;

public class HitAuditLog {
    private Long id;
    private String actor;
    private Platform platform;
    private String sourceType;
    private String content;
    private String hitWord;
    private String action;
    private LocalDateTime actionTime;

    public HitAuditLog() {
    }

    public HitAuditLog(Long id, String actor, Platform platform, String sourceType, String content, String hitWord, String action, LocalDateTime actionTime) {
        this.id = id;
        this.actor = actor;
        this.platform = platform;
        this.sourceType = sourceType;
        this.content = content;
        this.hitWord = hitWord;
        this.action = action;
        this.actionTime = actionTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getHitWord() {
        return hitWord;
    }

    public void setHitWord(String hitWord) {
        this.hitWord = hitWord;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }
}
