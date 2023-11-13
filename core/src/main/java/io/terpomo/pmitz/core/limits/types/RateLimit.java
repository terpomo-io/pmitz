package io.terpomo.pmitz.core.limits.types;

import io.terpomo.pmitz.core.limits.UsageLimit;

import java.util.concurrent.TimeUnit;

public class RateLimit extends UsageLimit {

    private int quota;

    private TimeUnit interval;

    private int duration;

    public RateLimit(int quota, TimeUnit interval, int duration) {
        this.quota = quota;
        this.interval = interval;
        this.duration = duration;
    }

    @Override
    public long getValue() {
        return quota;
    }

}
