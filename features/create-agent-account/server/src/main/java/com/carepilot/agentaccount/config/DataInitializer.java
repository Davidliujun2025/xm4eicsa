package com.carepilot.agentaccount.config;

import com.carepilot.agentaccount.model.AgentAccount;
import com.carepilot.agentaccount.repository.AgentAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    @ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
    CommandLineRunner initDatabase(
            AgentAccountRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.name}") String name,
            @Value("${app.bootstrap-admin.phone}") String phone,
            @Value("${app.bootstrap-admin.email}") String email,
            @Value("${app.bootstrap-admin.password}") String password
    ) {
        return args -> {
            if (repository.countByRole("SYSTEM_ADMIN") > 0) {
                return;
            }
            if (password == null || password.length() < 12) {
                throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD must contain at least 12 characters");
            }

            AgentAccount admin = new AgentAccount();
            admin.setName(name);
            admin.setPhone(phone);
            admin.setEmail(email);
            admin.setPasswordHash(passwordEncoder.encode(password));
            admin.setRole("SYSTEM_ADMIN");
            admin.setStatus("ENABLED");
            repository.save(admin);
        };
    }
}
