package com.carepilot.infra.ai;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 框架阶段默认实现：返回占位文本。
 * AI 同学接入时创建 {@code DashScopeQwenClient implements QwenClient} 替换此 Bean。
 */
@Component
@RequiredArgsConstructor
public class StubQwenClient implements QwenClient {

    @Override
    public String call(String prompt) {
        return "{ \"body\": \"[AI 生成中...]\", \"label\": \"待生成\", \"tags\": [] }";
    }
}
