package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.repository.AiDialogStepRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationEventPublisher;

import java.sql.Timestamp;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private final AiDialogStepRecordRepository repository = mock(AiDialogStepRecordRepository.class);
    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final TokenService service = new TokenService(
            repository, jdbcTemplate, eventPublisher, 0, "Asia/Shanghai");

    @BeforeEach
    void setUp() {
        when(jdbcTemplate.query(any(String.class), any(org.springframework.jdbc.core.ResultSetExtractor.class), eq("6")))
                .thenAnswer(invocation -> {
                    org.springframework.jdbc.core.ResultSetExtractor<Long> extractor = invocation.getArgument(1);
                    ResultSet resultSet = mock(ResultSet.class);
                    when(resultSet.next()).thenReturn(true);
                    when(resultSet.getLong("daily_token_limit")).thenReturn(10_000L);
                    return extractor.extractData(resultSet);
                });
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("6"),
                any(Timestamp.class), any(Timestamp.class))).thenReturn(3_600L);
    }

    @Test
    void readsDailyAndSessionUsageFromAiDialogStepRecords() {
        when(repository.sumTotalTokensBySessionTaskId(1L)).thenReturn(900L);

        assertThat(service.getTodayUsedTokens("6")).isEqualTo(3_600);
        assertThat(service.getSessionUsedTokens(1L)).isEqualTo(900);
        assertThat(service.getUsagePercent("6")).isEqualTo(36.0);
        assertThat(service.isTokenQuotaExceeded("6")).isFalse();
    }
}
