package com.carepilot.forbiddenwords.model;

import java.time.LocalDateTime;

public class OperationAuditLog {
    private Long id;
    private String action;
    private String operator;
    private String operatorIp;
    private String targetWord;
    private Platform platform;
    private LocalDateTime operationTime;

    public OperationAuditLog() {
    }

    public OperationAuditLog(Long id, String action, String operator, String operatorIp, String targetWord, Platform platform, LocalDateTime operationTime) {
        this.id = id;
        this.action = action;
        this.operator = operator;
        this.operatorIp = operatorIp;
        this.targetWord = targetWord;
        this.platform = platform;
        this.operationTime = operationTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperatorIp() {
        return operatorIp;
    }

    public void setOperatorIp(String operatorIp) {
        this.operatorIp = operatorIp;
    }

    public String getTargetWord() {
        return targetWord;
    }

    public void setTargetWord(String targetWord) {
        this.targetWord = targetWord;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    public LocalDateTime getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(LocalDateTime operationTime) {
        this.operationTime = operationTime;
    }
}
