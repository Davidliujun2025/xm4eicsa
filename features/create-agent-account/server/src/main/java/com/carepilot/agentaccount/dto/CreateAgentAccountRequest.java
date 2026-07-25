package com.carepilot.agentaccount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;

public class CreateAgentAccountRequest {

    @NotBlank(message = "请输入用户姓名")
    @Size(min = 2, max = 20, message = "用户姓名长度应为2–20个字符")
    @Pattern(
        regexp = "^[\\p{IsHan}A-Za-z]+$",
        message = "用户姓名仅支持中文、英文及中英文组合"
    )
    private String name;

@NotBlank(message = "请输入手机号")
@Pattern(
    regexp = "^1[3-9]\\d{9}$",
    message = "手机号格式错误"
)
private String phone;

@Email(message = "邮箱格式错误")
private String email;
@NotBlank(message = "请输入初始密码")
@Size(min = 12, max = 20, message = "初始密码长度应为12-20位")
@Pattern(
    regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{12,20}$",
    message = "初始密码必须包含字母和数字，且不能包含空格"
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