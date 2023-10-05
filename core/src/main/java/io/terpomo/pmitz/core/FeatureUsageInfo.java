package io.terpomo.pmitz.core;

import java.util.Map;

public record FeatureUsageInfo (FeatureStatus featureStatus, Map<String, Long> remainingUsageUnits) {

}