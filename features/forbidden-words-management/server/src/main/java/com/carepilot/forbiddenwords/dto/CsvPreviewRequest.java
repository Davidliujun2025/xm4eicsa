package com.carepilot.forbiddenwords.dto;

import jakarta.validation.constraints.NotBlank;

public class CsvPreviewRequest {
    @NotBlank
    private String csvContent;

    public String getCsvContent() {
        return csvContent;
    }

    public void setCsvContent(String csvContent) {
        this.csvContent = csvContent;
    }
}
