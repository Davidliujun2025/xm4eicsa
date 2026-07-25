package com.carepilot.common;

public final class ErrorCode {
    public static final String OK = "OK";
    public static final String PARAM_INVALID = "PARAM_INVALID";
    public static final String USERNAME_INVALID = "USERNAME_INVALID";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String RESET_TOKEN_INVALID = "RESET_TOKEN_INVALID";
    public static final String PHONE_REQUIRED = "PHONE_REQUIRED";
    public static final String PHONE_NOT_BOUND = "PHONE_NOT_BOUND";
    public static final String PHONE_NOT_MATCH = "PHONE_NOT_MATCH";
    public static final String SMS_CODE_COOLDOWN = "SMS_CODE_COOLDOWN";
    public static final String SMS_CODE_EMPTY = "SMS_CODE_EMPTY";
    public static final String SMS_CODE_INVALID = "SMS_CODE_INVALID";
    public static final String PASSWORD_NOT_MATCH = "PASSWORD_NOT_MATCH";
    public static final String PASSWORD_INVALID = "PASSWORD_INVALID";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    private ErrorCode() {
    }
}
