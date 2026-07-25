package com.carepilot.tokenquota.common;

public final class ErrorCode {
    public static final String OK = "OK";
    public static final String PARAM_INVALID = "PARAM_INVALID";
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    public static final String QUOTA_INVALID = "QUOTA_INVALID";
    public static final String USAGE_INVALID = "USAGE_INVALID";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    private ErrorCode() {
    }
}
