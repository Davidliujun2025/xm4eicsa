package com.carepilot.agentaccount.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UpdateStatusRequest {
    @NotNull(message = "请选择账号状态")
    @Pattern(regexp = "ENABLED|DISABLED", message = "账号状态无效")
    private String status;
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
