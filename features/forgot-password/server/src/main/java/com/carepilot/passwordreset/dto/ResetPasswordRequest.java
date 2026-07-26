package com.carepilot.passwordreset.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "重置凭证不能为空")
        String resetToken,

        @NotBlank(message = "请输入绑定手机号")
        String phone,

        @NotBlank(message = "请输入短信验证码")
        String smsCode,

        @NotBlank(message = "请输入新密码")
        String newPassword,

        @NotBlank(message = "请再次输入新密码")
        String confirmPassword
) {
}
