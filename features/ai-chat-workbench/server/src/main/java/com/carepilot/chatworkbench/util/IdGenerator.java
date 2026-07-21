package com.carepilot.chatworkbench.util;

import java.util.UUID;

public class IdGenerator {

    public static String generateConversationId() {
        return "conv_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static String generateCustomerId() {
        return "cust_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}