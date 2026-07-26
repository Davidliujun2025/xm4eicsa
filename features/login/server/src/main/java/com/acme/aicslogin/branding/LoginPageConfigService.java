package com.acme.aicslogin.branding;

import com.acme.aicslogin.api.BusinessException;
import com.acme.aicslogin.auth.dto.LoginPageConfigResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginPageConfigService {

    private static final long SINGLETON_ID = 1L;

    private final LoginPageConfigRepository repository;

    public LoginPageConfigService(LoginPageConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LoginPageConfigResponse getConfig() {
        LoginPageConfig config = repository.findById(SINGLETON_ID)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "LOGIN_CONFIG_UNAVAILABLE",
                        "登录页配置暂不可用"
                ));
        return LoginPageConfigResponse.from(config);
    }
}
