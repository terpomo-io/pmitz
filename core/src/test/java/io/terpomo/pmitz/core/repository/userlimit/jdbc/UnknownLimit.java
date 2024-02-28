package io.terpomo.pmitz.core.repository.userlimit.jdbc;

import java.time.ZonedDateTime;
import java.util.Optional;

import io.terpomo.pmitz.core.limits.UsageLimit;

public class UnknownLimit extends UsageLimit {

    private long value;

    public UnknownLimit(String id, long value) {
        super(id);
        this.value = value;
    }

    @Override
    public long getValue() {
        return value;
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