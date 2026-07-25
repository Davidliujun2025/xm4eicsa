package com.carepilot.passwordreset.controller;

import com.carepilot.common.ApiResponse;
import com.carepilot.passwordreset.dto.ResetPasswordRequest;
import com.carepilot.passwordreset.dto.ResetPasswordResponse;
import com.carepilot.passwordreset.dto.SendSmsCodeRequest;
import com.carepilot.passwordreset.dto.SendSmsCodeResponse;
import com.carepilot.passwordreset.dto.VerifyAccountRequest;
import com.carepilot.passwordreset.dto.VerifyAccountResponse;
import com.carepilot.passwordreset.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/password")
public class PasswordResetController {
    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/account/verify")
    public ApiResponse<VerifyAccountResponse> verifyAccount(
            @Valid @RequestBody VerifyAccountRequest request
    ) {
        return ApiResponse.ok(passwordResetService.verifyAccount(request));
    }

    @PostMapping("/sms-code")
    public ApiResponse<SendSmsCodeResponse> sendSmsCode(
            @Valid @RequestBody SendSmsCodeRequest request
    ) {
        return ApiResponse.ok("验证码已发送", passwordResetService.sendSmsCode(request));
    }

    @PostMapping("/confirm")
    public ApiResponse<ResetPasswordResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ApiResponse.ok("密码重置成功", passwordResetService.resetPassword(request));
    }
}
