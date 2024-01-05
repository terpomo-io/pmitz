package io.terpomo.pmitz.core.limits.types;

import io.terpomo.pmitz.core.limits.UsageLimit;

import java.time.ZonedDateTime;
import java.util.Optional;

public class CountLimit extends UsageLimit {

    private long count;

    public CountLimit(String id, long count) {
        this.count = count;
        setId(id);
    }

    @Override
    public long getValue() {
        return count;
    }

    @Override
    public Optional<ZonedDateTime> getWindowStart(ZonedDateTime referenceDate) {
        return Optional.empty();
    }

    @Override
    public Optional<ZonedDateTime> getWindowEnd(ZonedDateTime referenceDate) {
        return Optional.empty();
    }
}
