package com.carepilot.forbiddenwords.service;

import com.carepilot.forbiddenwords.model.ForbiddenWord;
import com.carepilot.forbiddenwords.model.Platform;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class HitAuditServiceTest {

    @Test
    void recordsOnlySelectedPlatformAndAllPlatformHits() {
        DataStore store = mock(DataStore.class);
        ForbiddenWordCacheService cache = new ForbiddenWordCacheService(5);
        cache.refresh(List.of(
                word(1L, "平台专属词", Platform.TAOBAO),
                word(2L, "通用词", Platform.ALL),
                word(3L, "其他平台词", Platform.JD)));
        HitAuditService service = new HitAuditService(store, cache);

        List<String> hits = service.recordHits(
                "6", Platform.TAOBAO, "USER_QUESTION",
                "包含平台专属词和通用词，也包含其他平台词", "RECORD_ONLY");

        assertThat(hits).containsExactly("平台专属词", "通用词");
        verify(store).addHitLog(eq("6"), eq(Platform.TAOBAO), eq("USER_QUESTION"),
                any(), eq("平台专属词"), eq("RECORD_ONLY"), any(LocalDateTime.class));
        verify(store).addHitLog(eq("6"), eq(Platform.TAOBAO), eq("USER_QUESTION"),
                any(), eq("通用词"), eq("RECORD_ONLY"), any(LocalDateTime.class));
        verify(store, never()).addHitLog(eq("6"), eq(Platform.TAOBAO), eq("USER_QUESTION"),
                any(), eq("其他平台词"), eq("RECORD_ONLY"), any(LocalDateTime.class));
    }

    private ForbiddenWord word(Long id, String value, Platform platform) {
        return new ForbiddenWord(id, value, platform, LocalDateTime.now(), "admin");
    }
}
