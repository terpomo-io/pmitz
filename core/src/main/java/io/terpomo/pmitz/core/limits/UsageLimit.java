package io.terpomo.pmitz.core.limits;

import java.time.ZonedDateTime;
import java.util.Optional;

public abstract class UsageLimit {

    private String unit;
    private String id;

    public abstract int getValue();

    public abstract Optional<ZonedDateTime> getWindowStart (ZonedDateTime windowEnd);

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
