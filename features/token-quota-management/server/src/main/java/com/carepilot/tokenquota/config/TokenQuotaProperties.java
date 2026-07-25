package com.carepilot.tokenquota.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "carepilot.token-quota")
public class TokenQuotaProperties {
    private int defaultPageSize = 20;
    private int maxPageSize = 100;
    private long maxDailyQuota = 10_000_000L;
    private double highUsageThreshold = 0.8D;

    public int getDefaultPageSize() {
        return defaultPageSize;
    }

    public void setDefaultPageSize(int defaultPageSize) {
        this.defaultPageSize = defaultPageSize;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public void setMaxPageSize(int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    public long getMaxDailyQuota() {
        return maxDailyQuota;
    }

    public void setMaxDailyQuota(long maxDailyQuota) {
        this.maxDailyQuota = maxDailyQuota;
    }

    public double getHighUsageThreshold() {
        return highUsageThreshold;
    }

    public void setHighUsageThreshold(double highUsageThreshold) {
        this.highUsageThreshold = highUsageThreshold;
    }
}
