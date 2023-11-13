package io.terpomo.pmitz.core.limits.types;

import io.terpomo.pmitz.core.limits.UsageLimit;

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

}
