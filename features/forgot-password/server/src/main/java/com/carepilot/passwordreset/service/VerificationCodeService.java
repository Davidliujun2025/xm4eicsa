package com.carepilot.passwordreset.service;

import com.carepilot.common.BusinessException;
import com.carepilot.common.ErrorCode;
import com.carepilot.passwordreset.config.PasswordResetProperties;
import com.carepilot.passwordreset.domain.ResetSession;
import com.carepilot.passwordreset.domain.VerificationCodeRecord;
import com.carepilot.passwordreset.dto.SendSmsCodeResponse;
import com.carepilot.passwordreset.repository.VerificationCodeRepository;
import com.carepilot.passwordreset.sms.SmsSender;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VerificationCodeService {
    private final PasswordResetProperties properties;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;
    private final SmsSender smsSender;
    private final VerificationCodeRepository verificationCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationCodeService(
            PasswordResetProperties properties,
            Clock clock,
            PasswordEncoder passwordEncoder,
            SmsSender smsSender,
            VerificationCodeRepository verificationCodeRepository
    ) {
        this.properties = properties;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
        this.smsSender = smsSender;
        this.verificationCodeRepository = verificationCodeRepository;
    }

    public SendSmsCodeResponse sendCode(ResetSession session, String phone) {
        cleanupExpiredCodes();
        Instant now = Instant.now(clock);
        VerificationCodeRecord existing = verificationCodeRepository
                .findLatestUnconsumed(session.token(), phone)
                .orElse(null);
        if (existing != null && existing.nextAllowedAt().isAfter(now)) {
            long remaining = Duration.between(now, existing.nextAllowedAt()).toSeconds();
            throw new BusinessException(
                    ErrorCode.SMS_CODE_COOLDOWN,
                    "验证码已发送，请" + Math.max(1, remaining) + "秒后再试"
            );
        }

        String code = generateCode(properties.getCodeLength());
        verificationCodeRepository.save(
                session.token(),
                session.accountId(),
                session.username(),
                phone,
                passwordEncoder.encode(code),
                now.plus(properties.getCodeTtl()),
                now.plus(properties.getCodeCooldown())
        );
        smsSender.sendPasswordResetCode(phone, code, properties.getCodeTtl());

        return new SendSmsCodeResponse(
                properties.getCodeTtl().toSeconds(),
                properties.getCodeCooldown().toSeconds()
        );
    }

    public void verifyAndConsume(ResetSession session, String phone, String inputCode) {
        cleanupExpiredCodes();
        if (!StringUtils.hasText(inputCode)) {
            throw new BusinessException(ErrorCode.SMS_CODE_EMPTY, "请输入短信验证码");
        }

        VerificationCodeRecord entry = verificationCodeRepository
                .findLatestUnconsumed(session.token(), phone)
                .orElse(null);
        Instant now = Instant.now(clock);
        if (entry == null || entry.expiresAt().isBefore(now)) {
            cleanupExpiredCodes();
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID, "验证码错误或已过期");
        }

        if (!passwordEncoder.matches(inputCode.trim(), entry.codeHash())) {
            throw new BusinessException(ErrorCode.SMS_CODE_INVALID, "验证码错误或已过期");
        }

        verificationCodeRepository.consume(entry.id());
    }

    private String generateCode(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(secureRandom.nextInt(10));
        }
        return builder.toString();
    }

    private void cleanupExpiredCodes() {
        verificationCodeRepository.cleanupExpired(Instant.now(clock));
    }
}
