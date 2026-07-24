package com.acme.aicslogin.branding;

import com.acme.aicslogin.api.ApiResponse;
import com.acme.aicslogin.auth.dto.LoginPageConfigResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class LoginPageConfigController {

    private final LoginPageConfigService service;

    public LoginPageConfigController(LoginPageConfigService service) {
        this.service = service;
    }

    @GetMapping("/login-config")
    public ApiResponse<LoginPageConfigResponse> getLoginConfig(CsrfToken csrfToken) {
        csrfToken.getToken();
        return ApiResponse.ok(service.getConfig());
    }
}
