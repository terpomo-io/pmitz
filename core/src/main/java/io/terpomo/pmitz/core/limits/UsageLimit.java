package io.terpomo.pmitz.core.limits;

import java.time.ZonedDateTime;
import java.util.Optional;

public abstract class UsageLimit {

    private String unit;
    private String id;

    public UsageLimit(String id) {
        this.id = id;
    }

    public UsageLimit(String id, String unit) {
        this.id = id;
        this.unit = unit;
    }

    public abstract long getValue();

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

    public abstract Optional<ZonedDateTime> getWindowStart(ZonedDateTime referenceDate);

    public abstract Optional<ZonedDateTime> getWindowEnd(ZonedDateTime referenceDate);
}
