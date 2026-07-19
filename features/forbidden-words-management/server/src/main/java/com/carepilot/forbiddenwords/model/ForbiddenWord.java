package com.carepilot.forbiddenwords.model;

import java.time.LocalDateTime;

public class ForbiddenWord {
    private Long id;
    private String word;
    private Platform platform;
    private LocalDateTime createdAt;
    private String createdBy;

    public ForbiddenWord() {
    }

    public ForbiddenWord(Long id, String word, Platform platform, LocalDateTime createdAt, String createdBy) {
        this.id = id;
        this.word = word;
        this.platform = platform;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
