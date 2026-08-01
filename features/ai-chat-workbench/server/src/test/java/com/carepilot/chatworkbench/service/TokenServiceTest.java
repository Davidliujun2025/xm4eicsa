package com.carepilot.chatworkbench.service;

import com.carepilot.chatworkbench.repository.AiDialogStepRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private final AiDialogStepRecordRepository repository = mock(AiDialogStepRecordRepository.class);
    private final TokenService service = new TokenService(repository);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "dailyLimit", 10_000);
    }

    @Test
    void readsDailyAndSessionUsageFromAiDialogStepRecords() {
        when(repository.sumTotalTokens(eq(6L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3_600L);
        when(repository.sumTotalTokensBySessionTaskId(1L)).thenReturn(900L);

        assertThat(service.getTodayUsedTokens("6")).isEqualTo(3_600);
        assertThat(service.getSessionUsedTokens(1L)).isEqualTo(900);
        assertThat(service.getUsagePercent("6")).isEqualTo(36.0);
        assertThat(service.isTokenQuotaExceeded("6")).isFalse();
    }
}
