package com.acme.aicslogin.auth.dto;

import com.acme.aicslogin.branding.LoginPageConfig;

import java.util.List;

public record LoginPageConfigResponse(
        String brandName,
        String logoUrl,
        String promoCopy,
        String forgotPasswordPath,
        PasswordPolicyResponse passwordPolicy
) {
    public static LoginPageConfigResponse from(LoginPageConfig config) {
        return new LoginPageConfigResponse(
                config.getBrandName(),
                config.getLogoUrl(),
                config.getPromoCopy(),
                "/forgot-password",
                new PasswordPolicyResponse(
                        12,
                        20,
                        3,
                        List.of("uppercase", "lowercase", "digit", "special"),
                        "密码长度12-20位，必须包含大写字母、小写字母、数字、特殊符号中的三类以上"
                )
        );
    }

    public record PasswordPolicyResponse(
            int minLength,
            int maxLength,
            int minCategories,
            List<String> categories,
            String hint
    ) {
    }
}
