package com.carepilot.agentaccount.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateAgentAccountRequest {

    @NotBlank(message = "请输入用户姓名")
    @Pattern(
            regexp = "^$|^(?=.{2,20}$)[\\p{IsHan}A-Za-z]+(?:\\s[\\p{IsHan}A-Za-z]+)*$",
            message = "用户姓名长度应为2～20个字符，支持中文、英文或中英文组合"
    )
    private String name;

    @Email(message = "请输入正确的邮箱地址")
    private String email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}