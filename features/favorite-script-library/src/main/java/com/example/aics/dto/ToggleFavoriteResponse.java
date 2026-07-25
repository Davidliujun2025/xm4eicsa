package com.example.aics.dto;

public record ToggleFavoriteResponse(
        boolean favorited,
        FavoriteResponse favorite,
        String message
) {
}
