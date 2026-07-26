package com.carepilot.passwordreset.repository;

import com.carepilot.passwordreset.domain.AccountRecord;
import java.util.Optional;

public interface AccountRepository {
    Optional<AccountRecord> findCustomerServiceByUsername(String username);

    Optional<AccountRecord> findCustomerServiceByPhone(String phone);

    void updatePasswordHash(Long accountId, String passwordHash);
}
