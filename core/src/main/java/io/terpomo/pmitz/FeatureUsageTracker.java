package io.terpomo.pmitz;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.subjects.UserGrouping;

import java.util.Map;

public interface FeatureUsageTracker {

    void recordFeatureUsage (Feature feature, UserGrouping userGrouping, Map<String, Long> requestedUnits);

    void reduceFeatureUsage (Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits);

    FeatureStatus getFeatureStatus (Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits);

}
