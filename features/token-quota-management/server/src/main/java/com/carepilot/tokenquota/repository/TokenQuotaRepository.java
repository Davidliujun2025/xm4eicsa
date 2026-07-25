package com.carepilot.tokenquota.repository;

import com.carepilot.tokenquota.domain.TokenQuotaAccount;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenQuotaRepository {
    List<TokenQuotaAccount> findAllAccounts();

    Optional<TokenQuotaAccount> findByAccountNo(String accountNo);

    TokenQuotaAccount save(TokenQuotaAccount account);

    default TokenQuotaAccount recordUsage(
            TokenQuotaAccount updatedAccount,
            long consumedTokens,
            long aiCallCount,
            long businessCount,
            LocalDateTime occurredAt
    ) {
        return save(updatedAccount);
    }
}
