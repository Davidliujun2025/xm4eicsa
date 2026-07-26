package com.carepilot.tokenquota.repository;

import com.carepilot.tokenquota.domain.TokenQuotaAccount;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("memory")
public class InMemoryTokenQuotaRepository implements TokenQuotaRepository {
    private final Clock clock;
    private final Map<String, TokenQuotaAccount> accounts = new ConcurrentHashMap<>();

    public InMemoryTokenQuotaRepository(Clock clock) {
        this.clock = clock;
        seedDemoAccounts();
    }

    private void seedDemoAccounts() {
        LocalDate today = LocalDate.now(clock);
        save(new TokenQuotaAccount(1L, "CS1001", "客服一号", 120_000, 32_000, 18, 9, today, true));
        save(new TokenQuotaAccount(2L, "CS1002", "客服二号", 100_000, 82_000, 37, 16, today, true));
        save(new TokenQuotaAccount(3L, "CS1003", "客服三号", 80_000, 91_000, 49, 20, today, true));
    }

    @Override
    public List<TokenQuotaAccount> findAllAccounts() {
        return accounts.values()
                .stream()
                .map(this::resetUsageIfNewDay)
                .sorted(Comparator.comparing(TokenQuotaAccount::accountNo))
                .toList();
    }

    @Override
    public Optional<TokenQuotaAccount> findByAccountNo(String accountNo) {
        TokenQuotaAccount account = accounts.get(normalize(accountNo));
        if (account == null) {
            return Optional.empty();
        }
        return Optional.of(resetUsageIfNewDay(account));
    }

    @Override
    public TokenQuotaAccount save(TokenQuotaAccount account) {
        accounts.put(normalize(account.accountNo()), account);
        return account;
    }

    @Override
    public TokenQuotaAccount recordUsage(
            TokenQuotaAccount updatedAccount,
            long consumedTokens,
            long aiCallCount,
            long businessCount,
            LocalDateTime occurredAt
    ) {
        return save(updatedAccount);
    }

    private TokenQuotaAccount resetUsageIfNewDay(TokenQuotaAccount account) {
        LocalDate today = LocalDate.now(clock);
        if (today.equals(account.usageDate())) {
            return account;
        }
        TokenQuotaAccount reset = account.withUsage(0, 0, 0, today);
        accounts.put(normalize(account.accountNo()), reset);
        return reset;
    }

    private String normalize(String accountNo) {
        return accountNo == null ? "" : accountNo.trim().toUpperCase();
    }
}
