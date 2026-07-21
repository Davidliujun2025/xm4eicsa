package com.carepilot.infra.auth;

import org.springframework.stereotype.Component;

/**
 * 用户身份获取。框架阶段 Mock 返回 userId=1。
 * 鉴权就绪后从请求 Token 中解析真实 userId。
 */
@Component
public class AuthContext {

    public Long getUserId() {
        // TODO: 鉴权就绪后从 SecurityContext / ThreadLocal / Header 中获取真实 userId
        return 1L;
    }
}
