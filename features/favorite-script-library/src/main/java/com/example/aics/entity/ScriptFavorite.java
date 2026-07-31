package com.example.aics.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(
        name = "script_favorite",
        indexes = {
                @Index(name = "idx_script_favorite_user_used", columnList = "user_id, last_used_at, id"),
                @Index(name = "idx_script_favorite_user_created", columnList = "user_id, created_at, id")
        }
)
public class ScriptFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long staffId;

    @Column(name = "source_talk_id", length = 160)
    private String sourceTalkId;

    @Column(name = "content_hash", nullable = false, length = 64, columnDefinition = "char(64)")
    private String contentHash;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "scenario", nullable = false, length = 100)
    private String scenario;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "script_favorite_tag",
            joinColumns = @JoinColumn(name = "favorite_id"),
            indexes = @Index(name = "idx_script_favorite_tag", columnList = "tag")
    )
    @Column(name = "tag", nullable = false, length = 5)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    protected ScriptFavorite() {
    }

    public ScriptFavorite(
            Long staffId,
            String sourceTalkId,
            String contentHash,
            String content,
            String scenario,
            Instant generatedAt,
            Set<String> tags,
            Instant now
    ) {
        this.staffId = staffId;
        this.sourceTalkId = sourceTalkId;
        this.contentHash = contentHash;
        this.content = content;
        this.scenario = scenario;
        this.generatedAt = generatedAt;
        this.tags = new LinkedHashSet<>(tags);
        this.createdAt = now;
        this.lastUsedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getStaffId() {
        return staffId;
    }

    public String getSourceTalkId() {
        return sourceTalkId;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getContent() {
        return content;
    }

    public String getScenario() {
        return scenario;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void markUsed(Instant now) {
        this.lastUsedAt = now;
    }
}
