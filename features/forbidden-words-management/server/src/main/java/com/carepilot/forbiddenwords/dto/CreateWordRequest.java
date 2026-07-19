package com.carepilot.forbiddenwords.dto;

import com.carepilot.forbiddenwords.model.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateWordRequest {
    @NotBlank
    private String word;

    @NotNull
    private Platform platform;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }
}
