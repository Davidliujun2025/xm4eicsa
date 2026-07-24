package com.acme.aicslogin.config;

import com.acme.aicslogin.user.CustomerServiceUser;
import com.acme.aicslogin.user.CustomerServiceUserRepository;
import com.acme.aicslogin.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevelopmentDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevelopmentDataInitializer.class);
    private static final String DEMO_ACCOUNT = "demo.agent";

    private final CustomerServiceUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public DevelopmentDataInitializer(CustomerServiceUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (repository.findByAccount(DEMO_ACCOUNT).isEmpty()) {
            repository.save(CustomerServiceUser.create(
                    DEMO_ACCOUNT,
                    passwordEncoder.encode("AiService2026!"),
                    "演示客服",
                    UserStatus.ACTIVE
            ));
            log.info("Created development-only demo account {}", DEMO_ACCOUNT);
        }
    }
}
