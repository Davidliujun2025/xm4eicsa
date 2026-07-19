package com.carepilot.forbiddenwords.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class CsvConfirmRequest {
    @NotEmpty
    private List<CsvPreviewItem> previewItems;

    public List<CsvPreviewItem> getPreviewItems() {
        return previewItems;
    }

    public void setPreviewItems(List<CsvPreviewItem> previewItems) {
        this.previewItems = previewItems;
    }
}
