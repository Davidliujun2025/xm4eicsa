package com.carepilot.passwordreset.service;

import com.carepilot.passwordreset.config.PasswordResetProperties;
import com.carepilot.passwordreset.domain.AccountRecord;
import com.carepilot.passwordreset.dto.ResetPasswordRequest;
import com.carepilot.passwordreset.dto.SendSmsCodeRequest;
import com.carepilot.passwordreset.dto.VerifyAccountRequest;
import com.carepilot.passwordreset.dto.VerifyAccountResponse;
import com.carepilot.passwordreset.repository.AccountRepository;
import com.carepilot.passwordreset.repository.InMemoryResetSessionRepository;
import com.carepilot.passwordreset.repository.InMemoryVerificationCodeRepository;
import com.carepilot.passwordreset.sms.SmsSender;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetServiceFlowTest {

    @Test
    void resetsPasswordThroughSmsCodeFlow() {
        PasswordResetProperties properties = new PasswordResetProperties();
        Clock clock = Clock.systemUTC();
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        CapturingSmsSender smsSender = new CapturingSmsSender();
        TestAccountRepository accountRepository = new TestAccountRepository(passwordEncoder);

        ResetTokenService resetTokenService = new ResetTokenService(
                properties,
                clock,
                new InMemoryResetSessionRepository()
        );
        VerificationCodeService verificationCodeService = new VerificationCodeService(
                properties,
                clock,
                passwordEncoder,
                smsSender,
                new InMemoryVerificationCodeRepository()
        );
        PasswordResetService service = new PasswordResetService(
                accountRepository,
                properties,
                resetTokenService,
                verificationCodeService,
                new PasswordPolicy(),
                passwordEncoder
        );

        VerifyAccountResponse verifyResponse = service.verifyAccount(new VerifyAccountRequest("13800000000"));
        service.sendSmsCode(new SendSmsCodeRequest(verifyResponse.resetToken(), "13800000000"));
        service.resetPassword(new ResetPasswordRequest(
                verifyResponse.resetToken(),
                "13800000000",
                smsSender.lastCode,
                "NewPassword123!",
                "NewPassword123!"
        ));

        assertTrue(accountRepository.passwordMatches(1L, "NewPassword123!"));
    }

    private static class CapturingSmsSender implements SmsSender {
        private String lastCode;

        @Override
        public void sendPasswordResetCode(String phone, String code, Duration ttl) {
            this.lastCode = code;
        }
    }

    private static class TestAccountRepository implements AccountRepository {
        private final PasswordEncoder passwordEncoder;
        private final Map<Long, AccountRecord> accounts = new ConcurrentHashMap<>();

        private TestAccountRepository(PasswordEncoder passwordEncoder) {
            this.passwordEncoder = passwordEncoder;
            accounts.put(1L, new AccountRecord(
                    1L,
                    "13800000000",
                    "13800000000",
                    passwordEncoder.encode("DemoPass123!"),
                    true
            ));
        }

        @Override
        public Optional<AccountRecord> findCustomerServiceByUsername(String username) {
            return accounts.values()
                    .stream()
                    .filter(account -> account.username().equalsIgnoreCase(username))
                    .findFirst();
        }

        @Override
        public Optional<AccountRecord> findCustomerServiceByPhone(String phone) {
            return accounts.values()
                    .stream()
                    .filter(account -> account.phone().equals(phone))
                    .findFirst();
        }

        @Override
        public void updatePasswordHash(Long accountId, String passwordHash) {
            accounts.computeIfPresent(accountId, (id, old) -> new AccountRecord(
                    old.id(),
                    old.username(),
                    old.phone(),
                    passwordHash,
                    old.enabled()
            ));
        }

        private boolean passwordMatches(Long accountId, String rawPassword) {
            return passwordEncoder.matches(rawPassword, accounts.get(accountId).passwordHash());
        }
    }
}
