package com.carepilot.passwordreset.dto;

import jakarta.validation.constraints.NotBlank;

public record SendSmsCodeRequest(
        @NotBlank(message = "重置凭证不能为空")
        String resetToken,

        @NotBlank(message = "请输入绑定手机号")
        String phone
) {
}
