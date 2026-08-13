package com.carepilot.forbiddenwords.service;

import com.carepilot.forbiddenwords.dto.ForbiddenWordStats;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ForbiddenWordServiceStatsTest {

    @Test
    void returnsDatabaseBackedWordPlatformAndTodayHitCounts() {
        DataStore store = mock(DataStore.class);
        ForbiddenWordCacheService cache = new ForbiddenWordCacheService(5);
        when(store.countWords()).thenReturn(12L);
        when(store.countCoveredPlatforms()).thenReturn(4L);
        when(store.countHitsOnDate(any())).thenReturn(9L);
        ForbiddenWordService service = new ForbiddenWordService(store, cache);

        ForbiddenWordStats stats = service.stats();

        assertThat(stats.totalWords()).isEqualTo(12L);
        assertThat(stats.coveredPlatforms()).isEqualTo(4L);
        assertThat(stats.todayTriggerCount()).isEqualTo(9L);
    }
}
