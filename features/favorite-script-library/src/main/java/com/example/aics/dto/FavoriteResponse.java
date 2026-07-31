package com.example.aics.dto;

import com.example.aics.entity.ScriptFavorite;

import java.time.Instant;
import java.util.List;

public record FavoriteResponse(
        Long id,
        String sourceTalkId,
        String content,
        String scenario,
        Instant generatedAt,
        List<String> tags,
        Instant createdAt,
        Instant lastUsedAt
) {
    public static FavoriteResponse from(ScriptFavorite favorite) {
        return new FavoriteResponse(
                favorite.getId(),
                favorite.getSourceTalkId(),
                favorite.getContent(),
                favorite.getScenario(),
                favorite.getGeneratedAt(),
                List.copyOf(favorite.getTags()),
                favorite.getCreatedAt(),
                favorite.getLastUsedAt()
        );
    }
}
