package com.carepilot.passwordreset.service;

import com.carepilot.common.BusinessException;
import com.carepilot.common.ErrorCode;
import com.carepilot.passwordreset.config.PasswordResetProperties;
import com.carepilot.passwordreset.domain.AccountRecord;
import com.carepilot.passwordreset.domain.ResetSession;
import com.carepilot.passwordreset.dto.ResetPasswordRequest;
import com.carepilot.passwordreset.dto.ResetPasswordResponse;
import com.carepilot.passwordreset.dto.SendSmsCodeRequest;
import com.carepilot.passwordreset.dto.SendSmsCodeResponse;
import com.carepilot.passwordreset.dto.VerifyAccountRequest;
import com.carepilot.passwordreset.dto.VerifyAccountResponse;
import com.carepilot.passwordreset.repository.AccountRepository;
import com.carepilot.passwordreset.util.MaskingUtils;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PasswordResetService {
    private final AccountRepository accountRepository;
    private final PasswordResetProperties properties;
    private final ResetTokenService resetTokenService;
    private final VerificationCodeService verificationCodeService;
    private final PasswordPolicy passwordPolicy;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            AccountRepository accountRepository,
            PasswordResetProperties properties,
            ResetTokenService resetTokenService,
            VerificationCodeService verificationCodeService,
            PasswordPolicy passwordPolicy,
            PasswordEncoder passwordEncoder
    ) {
        this.accountRepository = accountRepository;
        this.properties = properties;
        this.resetTokenService = resetTokenService;
        this.verificationCodeService = verificationCodeService;
        this.passwordPolicy = passwordPolicy;
        this.passwordEncoder = passwordEncoder;
    }

    public VerifyAccountResponse verifyAccount(VerifyAccountRequest request) {
        String username = normalize(request.username());
        if (!Pattern.matches(properties.getUsernamePattern(), username)) {
            throw new BusinessException(ErrorCode.USERNAME_INVALID, "用户名格式不正确");
        }

        AccountRecord account = accountRepository.findCustomerServiceByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在"));
        ensureEnabled(account);

        ResetSession session = resetTokenService.createSession(account);
        return new VerifyAccountResponse(
                session.token(),
                account.username(),
                MaskingUtils.maskPhone(account.phone()),
                properties.getResetTokenTtl().toSeconds()
        );
    }

    public SendSmsCodeResponse sendSmsCode(SendSmsCodeRequest request) {
        ResetSession session = resetTokenService.getRequiredSession(request.resetToken());
        String phone = normalize(request.phone());
        AccountRecord account = ensurePhoneBelongsToSession(session, phone);
        ensureEnabled(account);
        return verificationCodeService.sendCode(session, phone);
    }

    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        ResetSession session = resetTokenService.getRequiredSession(request.resetToken());
        String phone = normalize(request.phone());
        AccountRecord account = ensurePhoneBelongsToSession(session, phone);
        ensureEnabled(account);

        verificationCodeService.verifyAndConsume(session, phone, request.smsCode());
        passwordPolicy.validate(request.newPassword(), request.confirmPassword());
        accountRepository.updatePasswordHash(account.id(), passwordEncoder.encode(request.newPassword()));
        resetTokenService.consume(session.token());

        return new ResetPasswordResponse(true, "BACK_TO_LOGIN");
    }

    private AccountRecord ensurePhoneBelongsToSession(ResetSession session, String phone) {
        if (!StringUtils.hasText(phone)) {
            throw new BusinessException(ErrorCode.PHONE_REQUIRED, "请输入绑定手机号");
        }

        AccountRecord account = accountRepository.findCustomerServiceByPhone(phone)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PHONE_NOT_BOUND,
                        "该手机号未绑定任何客服账号"
                ));
        if (!account.id().equals(session.accountId())) {
            throw new BusinessException(ErrorCode.PHONE_NOT_MATCH, "该手机号未绑定当前账号");
        }
        return account;
    }

    private void ensureEnabled(AccountRecord account) {
        if (!account.enabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "账号已禁用，请联系管理员");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
