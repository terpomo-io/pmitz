package io.terpomo.pmitz.core.limits;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;

import java.util.Map;

public interface UsageLimitVerifier {

    boolean isWithinLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits);

    void recordFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits);

    void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits);
}
