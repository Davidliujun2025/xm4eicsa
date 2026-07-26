package com.carepilot.tokenquota.domain;

public enum TokenUsageStatus {
    NORMAL("NORMAL", "正常"),
    HIGH_USAGE("HIGH_USAGE", "Token 使用率较高"),
    EXCEEDED_RECOMMENDED("EXCEEDED_RECOMMENDED", "已超出建议额度");

    private final String code;
    private final String label;

    TokenUsageStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
