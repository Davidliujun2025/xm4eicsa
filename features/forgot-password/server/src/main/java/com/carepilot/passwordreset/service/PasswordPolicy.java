package com.carepilot.passwordreset.service;

import com.carepilot.common.BusinessException;
import com.carepilot.common.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    private static final String PASSWORD_RULE_MESSAGE =
            "密码需为12-20位，且大写字母、小写字母、数字、特殊符号中至少包含三类";

    public void validate(String newPassword, String confirmPassword) {
        if (newPassword == null || confirmPassword == null) {
            throw new BusinessException(ErrorCode.PASSWORD_INVALID, PASSWORD_RULE_MESSAGE);
        }

        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH, "两次输入的新密码不一致");
        }

        int length = newPassword.codePointCount(0, newPassword.length());
        if (length < 12 || length > 20 || containsWhitespace(newPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_INVALID, PASSWORD_RULE_MESSAGE);
        }

        int categories = 0;
        if (hasUppercase(newPassword)) {
            categories++;
        }
        if (hasLowercase(newPassword)) {
            categories++;
        }
        if (hasDigit(newPassword)) {
            categories++;
        }
        if (hasSpecial(newPassword)) {
            categories++;
        }

        if (categories < 3) {
            throw new BusinessException(ErrorCode.PASSWORD_INVALID, PASSWORD_RULE_MESSAGE);
        }
    }

    private boolean containsWhitespace(String value) {
        return value.chars().anyMatch(Character::isWhitespace);
    }

    private boolean hasUppercase(String value) {
        return value.chars().anyMatch(Character::isUpperCase);
    }

    private boolean hasLowercase(String value) {
        return value.chars().anyMatch(Character::isLowerCase);
    }

    private boolean hasDigit(String value) {
        return value.chars().anyMatch(Character::isDigit);
    }

    private boolean hasSpecial(String value) {
        return value.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch));
    }
}
