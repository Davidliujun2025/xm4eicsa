package com.carepilot.chatworkbench.dto.response;

public record EvaluationStatsResponse(
        int usedToday,
        int remainingToday,
        int dailyLimit,
        long cumulativeReceptionCount,
        double cumulativeAverageScore,
        long cumulativeTokenUsage
) { }
