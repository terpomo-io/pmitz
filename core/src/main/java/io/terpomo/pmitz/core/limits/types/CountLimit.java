package io.terpomo.pmitz.core.limits.types;

import io.terpomo.pmitz.core.limits.UsageLimit;

import java.time.ZonedDateTime;
import java.util.Optional;

public class CountLimit extends UsageLimit {

    private int count;


    @Override
    public int getValue() {
        return count;
    }

    @Override
    public Optional<ZonedDateTime> getWindowStart(ZonedDateTime windowEnd) {
        return Optional.empty();
    }
}
