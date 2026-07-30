package com.acme.aicslogin.branding;

import com.acme.aicslogin.api.BusinessException;
import com.acme.aicslogin.auth.dto.LoginPageConfigResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginPageConfigService {

    private static final long SINGLETON_ID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LoginPageConfigService.class);

    private final LoginPageConfigRepository repository;

    public LoginPageConfigService(LoginPageConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LoginPageConfigResponse getConfig() {
        try {
            return repository.findById(SINGLETON_ID)
                .map(LoginPageConfigResponse::from)
                .orElseGet(this::defaultConfig);
        } catch (DataAccessException ex) {
            log.warn("Failed to load login page config from DB, fallback to default config", ex);
            return defaultConfig();
        }
        }

        private LoginPageConfigResponse defaultConfig() {
        return new LoginPageConfigResponse(
            "AI客服工作台",
            "/assets/brand-logo.svg",
            "AI智能客服，快速响应每一位买家",
            "/forgot-password",
            new LoginPageConfigResponse.PasswordPolicyResponse(
                12,
                20,
                3,
                java.util.List.of("uppercase", "lowercase", "digit", "special"),
                "密码长度12-20位，必须包含大写字母、小写字母、数字、特殊符号中的三类以上"
            )
        );
    }
}
