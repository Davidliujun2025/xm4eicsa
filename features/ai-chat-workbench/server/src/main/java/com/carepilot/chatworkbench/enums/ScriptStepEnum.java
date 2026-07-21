package com.carepilot.chatworkbench.enums;

public enum ScriptStepEnum {

    INTENT_RECOGNITION("intentRecognition", "意图识别"),
    REPLY_STRATEGY("replyStrategy", "回复策略"),
    RECOMMENDED_SCRIPT("recommendedScript", "推荐话术"),
    HOOK_GUIDANCE("hookGuidance", "钩子引导"),
    SUCCESS_CLOSE("successClose", "成功收尾");

    private final String code;
    private final String description;

    ScriptStepEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static ScriptStepEnum fromCode(String code) {
        for (ScriptStepEnum step : values()) {
            if (step.code.equalsIgnoreCase(code)) {
                return step;
            }
        }
        throw new IllegalArgumentException("Unknown script step code: " + code);
    }
}