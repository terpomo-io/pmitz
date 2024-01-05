package io.terpomo.pmitz.core.limits.types;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class SlidingWindowRateLimit extends RateLimit{

    public SlidingWindowRateLimit(int quota, ChronoUnit interval, int duration) {
        super(quota, interval, duration);
    }

    @Override
    public Optional<ZonedDateTime> getWindowStart(ZonedDateTime referenceDate) {
        return Optional.of(referenceDate.minus(getDuration(), getInterval()));
    }

    @Override
    public Optional<ZonedDateTime> getWindowEnd(ZonedDateTime referenceDate) {
        return Optional.of(referenceDate);
    }
}
