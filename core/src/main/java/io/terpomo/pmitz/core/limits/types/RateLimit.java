package io.terpomo.pmitz.core.limits.types;

import io.terpomo.pmitz.core.limits.UsageLimit;

import java.time.Period;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Optional;
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
    public int getValue() {
        return quota;
    }

    @Override
    public Optional<ZonedDateTime> getWindowStart(ZonedDateTime windowEnd) {
        return Optional.of(windowEnd.minus(interval.toSeconds(duration), ChronoUnit.SECONDS));
    }
}
