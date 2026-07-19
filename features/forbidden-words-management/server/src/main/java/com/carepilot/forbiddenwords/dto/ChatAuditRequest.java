package com.carepilot.forbiddenwords.dto;

import com.carepilot.forbiddenwords.model.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ChatAuditRequest {
    @NotBlank
    private String actor;

    @NotNull
    private Platform platform;

    @NotBlank
    private String userQuestion;

    @NotBlank
    private String aiAnswer;

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public String getUserQuestion() {
        return userQuestion;
    }

    public void setUserQuestion(String userQuestion) {
        this.userQuestion = userQuestion;
    }

    public String getAiAnswer() {
        return aiAnswer;
    }

    public void setAiAnswer(String aiAnswer) {
        this.aiAnswer = aiAnswer;
    }
}
