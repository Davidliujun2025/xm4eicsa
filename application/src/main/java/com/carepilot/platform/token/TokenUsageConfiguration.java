package com.carepilot.platform.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tokenmonitor.JdbcTokenQuotaResolver;
import tokenmonitor.JdbcTokenUsageRepository;
import tokenmonitor.TokenQuotaResolver;
import tokenmonitor.TokenUsageRepository;
import tokenmonitor.TokenUsageService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class TokenUsageConfiguration {

    @Bean
    TokenUsageRepository tokenUsageRepository(DataSource dataSource) {
        return new JdbcTokenUsageRepository(dataSource);
    }

    @Bean
    TokenQuotaResolver tokenQuotaResolver(
            DataSource dataSource,
            @Value("${app.token-monitor.default-daily-limit:0}") long defaultDailyLimit
    ) {
        return new JdbcTokenQuotaResolver(dataSource, defaultDailyLimit);
    }

    @Bean
    TokenUsageService tokenUsageService(
            TokenUsageRepository repository,
            TokenQuotaResolver quotaResolver,
            @Value("${app.token-monitor.zone:Asia/Shanghai}") String zone,
            @Value("${app.token-monitor.high-usage-threshold:0.80}") BigDecimal highUsageThreshold,
            @Value("${app.token-monitor.failure-rate-threshold:0.20}") BigDecimal failureRateThreshold
    ) {
        return new TokenUsageService(
                repository,
                ZoneId.of(zone),
                Clock.systemUTC(),
                quotaResolver,
                highUsageThreshold,
                failureRateThreshold);
    }
}
