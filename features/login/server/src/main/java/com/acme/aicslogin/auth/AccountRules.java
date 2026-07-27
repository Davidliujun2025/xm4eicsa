package com.acme.aicslogin.auth;

import java.util.Locale;
import java.util.regex.Pattern;

public final class AccountRules {

    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z0-9@._-]{4,64}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    private AccountRules() {
    }

    public static String normalize(String account) {
        return account == null ? "" : account.strip().toLowerCase(Locale.ROOT);
    }

    public static boolean isPhone(String normalizedInput) {
        return PHONE_PATTERN.matcher(normalizedInput).matches();
    }

    public static boolean isValid(String normalizedAccount) {
        return ACCOUNT_PATTERN.matcher(normalizedAccount).matches();
    }

    public static boolean isValidPhone(String normalizedPhone) {
        return PHONE_PATTERN.matcher(normalizedPhone).matches();
    }
}
