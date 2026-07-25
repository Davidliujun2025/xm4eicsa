package com.carepilot.passwordreset.repository;

import com.carepilot.passwordreset.domain.AccountRecord;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryAccountRepository implements AccountRepository {
    private final PasswordEncoder passwordEncoder;
    private final Map<Long, AccountRecord> accounts = new ConcurrentHashMap<>();

    public InMemoryAccountRepository(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    void seedDemoAccount() {
        accounts.put(1L, new AccountRecord(
                1L,
                "demo_cs",
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
}
