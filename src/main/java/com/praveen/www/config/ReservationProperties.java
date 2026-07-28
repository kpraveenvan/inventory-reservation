package com.praveen.www.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for reservation TTL and expiry sweep interval.
 */
@ConfigurationProperties(prefix = "reservation")
public class ReservationProperties {

    /**
     * How long an active reservation remains valid, in seconds.
     */
    private long ttlSeconds = 600;

    /**
     * Fixed delay between expiry sweep runs, in milliseconds.
     */
    private long expirySweepMs = 5000;

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public long getExpirySweepMs() {
        return expirySweepMs;
    }

    public void setExpirySweepMs(long expirySweepMs) {
        this.expirySweepMs = expirySweepMs;
    }
}
