package io.terpomo.pmitz.core;

import java.util.Map;

public record FeatureUsageInfo (FeatureStatus featureStatus, Map<String, Long> remainingUsageUnits) {

    public boolean isLimitReached(){
        return remainingUsageUnits != null && remainingUsageUnits.values().stream().anyMatch(v -> v <= 0);
    }

}