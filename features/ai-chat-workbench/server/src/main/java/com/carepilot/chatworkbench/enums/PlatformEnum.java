package com.carepilot.chatworkbench.enums;

public enum PlatformEnum {

    TAOBAO("淘宝"),
    TMALL("天猫"),
    JD("京东"),
    PINDUODUO("拼多多"),
    DOUYIN("抖音"),
    XIAOHONGSHU("小红书"),
    KUAISHOU("快手"),
    VIDEO_NUMBER("视频号"),
    WECHAT_SHOP("微信小店"),
    OTHER("其他平台");

    private final String description;

    PlatformEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static boolean isValid(String platform) {
        for (PlatformEnum p : values()) {
            if (p.name().equalsIgnoreCase(platform) || p.description.equalsIgnoreCase(platform)) {
                return true;
            }
        }
        return false;
    }
}