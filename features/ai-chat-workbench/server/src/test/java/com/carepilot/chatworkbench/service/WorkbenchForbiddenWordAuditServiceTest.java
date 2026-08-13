package com.carepilot.chatworkbench.service;

import com.carepilot.forbiddenwords.model.Platform;
import com.carepilot.forbiddenwords.service.HitAuditService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkbenchForbiddenWordAuditServiceTest {

    @Test
    void mapsEveryWorkbenchPlatformToForbiddenWordPlatform() {
        assertThat(WorkbenchForbiddenWordAuditService.resolvePlatform("淘宝")).isEqualTo(Platform.TAOBAO);
        assertThat(WorkbenchForbiddenWordAuditService.resolvePlatform("天猫")).isEqualTo(Platform.TMALL);
        assertThat(WorkbenchForbiddenWordAuditService.resolvePlatform("京东")).isEqualTo(Platform.JD);
        assertThat(WorkbenchForbiddenWordAuditService.resolvePlatform("拼多多")).isEqualTo(Platform.PINDUODUO);
        assertThat(WorkbenchForbiddenWordAuditService.resolvePlatform("抖音")).isEqualTo(Platform.DOUYIN);
        assertThat(WorkbenchForbiddenWordAuditService.resolvePlatform("小红书")).isEqualTo(Platform.XIAOHONGSHU);
        assertThat(WorkbenchForbiddenWordAuditService.resolvePlatform("快手")).isEqualTo(Platform.KUAISHOU);
        assertThat(WorkbenchForbiddenWordAuditService.resolvePlatform("视频号")).isEqualTo(Platform.SHIPINHAO);
        assertThat(WorkbenchForbiddenWordAuditService.resolvePlatform("微信小店")).isEqualTo(Platform.WECHAT_SHOP);
        assertThat(WorkbenchForbiddenWordAuditService.resolvePlatform("unknown")).isEqualTo(Platform.OTHER);
    }

    @Test
    void recordsManualEditAsRecordOnly() {
        HitAuditService hitAuditService = mock(HitAuditService.class);
        WorkbenchForbiddenWordAuditService service = new WorkbenchForbiddenWordAuditService(hitAuditService);

        service.auditManualEdit("6", "淘宝", "人工修改后的内容");

        verify(hitAuditService).recordHits(
                "6", Platform.TAOBAO, "MANUAL_EDIT", "人工修改后的内容", "RECORD_ONLY");
    }

    @Test
    void auditStorageFailureDoesNotInterruptWorkbench() {
        HitAuditService hitAuditService = mock(HitAuditService.class);
        when(hitAuditService.recordHits("6", Platform.JD, "AI_ANSWER", "生成内容", "RECORD_ONLY"))
                .thenThrow(new IllegalStateException("database unavailable"));
        WorkbenchForbiddenWordAuditService service = new WorkbenchForbiddenWordAuditService(hitAuditService);

        assertThatCode(() -> service.auditAiAnswer("6", "京东", "生成内容"))
                .doesNotThrowAnyException();
    }
}
