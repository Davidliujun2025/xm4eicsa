package com.carepilot.chatworkbench.service;

import com.carepilot.forbiddenwords.model.Platform;
import com.carepilot.forbiddenwords.service.HitAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

/** Non-blocking forbidden-word audit for chat workbench content. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkbenchForbiddenWordAuditService {

    static final String RECORD_ONLY_ACTION = "RECORD_ONLY";

    private final HitAuditService hitAuditService;

    public void auditUserQuestion(String actor, String platform, String content) {
        audit(actor, platform, "USER_QUESTION", content);
    }

    public void auditAiAnswer(String actor, String platform, String content) {
        audit(actor, platform, "AI_ANSWER", content);
    }

    public void auditManualEdit(String actor, String platform, String content) {
        audit(actor, platform, "MANUAL_EDIT", content);
    }

    private void audit(String actor, String platform, String sourceType, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        try {
            hitAuditService.recordHits(
                    actor, resolvePlatform(platform), sourceType, content, RECORD_ONLY_ACTION);
        } catch (RuntimeException exception) {
            log.warn("Forbidden-word audit failed: actor={}, platform={}, sourceType={}",
                    actor, platform, sourceType, exception);
        }
    }

    static Platform resolvePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return Platform.OTHER;
        }
        String value = platform.trim();
        return switch (value) {
            case "淘宝" -> Platform.TAOBAO;
            case "天猫" -> Platform.TMALL;
            case "京东" -> Platform.JD;
            case "拼多多" -> Platform.PINDUODUO;
            case "抖音" -> Platform.DOUYIN;
            case "小红书" -> Platform.XIAOHONGSHU;
            case "快手" -> Platform.KUAISHOU;
            case "视频号" -> Platform.SHIPINHAO;
            case "微信小店" -> Platform.WECHAT_SHOP;
            default -> resolveEnumName(value);
        };
    }

    private static Platform resolveEnumName(String value) {
        try {
            return Platform.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Platform.OTHER;
        }
    }
}
