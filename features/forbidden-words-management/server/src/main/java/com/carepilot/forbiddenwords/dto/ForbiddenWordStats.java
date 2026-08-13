package com.carepilot.forbiddenwords.dto;

public record ForbiddenWordStats(
        long totalWords,
        long coveredPlatforms,
        long todayTriggerCount
) {
}
