package com.carepilot.chatworkbench.dto.response;

import com.carepilot.chatworkbench.entity.CustomerServiceEvaluationReport;

import java.util.List;

public record EvaluationPageResponse(
        List<CustomerServiceEvaluationReport> items,
        long total,
        int page,
        int size,
        int totalPages
) { }
