package com.carepilot.agentaccount.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateAgentAccountRequest {

    @NotBlank(message = "请输入用户姓名")
    @Pattern(
            regexp = "^$|^(?=.{2,20}$)[\\p{IsHan}A-Za-z]+(?:\\s[\\p{IsHan}A-Za-z]+)*$",
            message = "用户姓名长度应为2–20个字符，支持中文、英文或中英文组合"
    )
    private String name;

    @NotBlank(message = "请输入手机号")
    @Pattern(
            regexp = "^$|^1[3-9]\\d{9}$",
            message = "请输入正确的中国大陆11位手机号"
    )
    private String phone;

    @Email(message = "请输入正确的邮箱地址")
    private String email;

    @NotBlank(message = "请输入初始密码")
    @Pattern(
            regexp = "^$|^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@#$%_!]{12,20}$",
            message = "初始密码必须为12～20位，包含字母和数字，仅支持字母、数字及 @、#、$、%、_、!"
    )
    private String initialPassword;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInitialPassword() {
        return initialPassword;
    }

    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }
}
