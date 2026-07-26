package com.acme.aicslogin.auth;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {

    public static final int MIN_LENGTH = 12;
    public static final int MAX_LENGTH = 20;
    public static final int MIN_CATEGORIES = 3;

    public boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            return false;
        }

        boolean upper = password.codePoints().anyMatch(Character::isUpperCase);
        boolean lower = password.codePoints().anyMatch(Character::isLowerCase);
        boolean digit = password.codePoints().anyMatch(Character::isDigit);
        boolean special = password.codePoints().anyMatch(value -> !Character.isLetterOrDigit(value));
        int categories = (upper ? 1 : 0) + (lower ? 1 : 0) + (digit ? 1 : 0) + (special ? 1 : 0);
        return categories >= MIN_CATEGORIES;
    }
}
