package com.carepilot.agentaccount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ResetPasswordRequest {

    @NotBlank(message = "请输入初始密码")
    @Pattern(
            regexp = "^$|^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@#$%_!]{12,20}$",
            message = "初始密码必须为12～20位，包含字母和数字，仅支持字母、数字及 @、#、$、%、_、!"
    )
    private String initialPassword;

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }
}