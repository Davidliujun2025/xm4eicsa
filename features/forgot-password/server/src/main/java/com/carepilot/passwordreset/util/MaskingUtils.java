package com.carepilot.passwordreset.util;

public final class MaskingUtils {
    private MaskingUtils() {
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
