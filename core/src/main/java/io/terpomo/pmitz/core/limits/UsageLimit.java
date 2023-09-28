package io.terpomo.pmitz.core.limits;

public abstract class UsageLimit {

    private String unit;
    private String id;

    public abstract int getValue();

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
