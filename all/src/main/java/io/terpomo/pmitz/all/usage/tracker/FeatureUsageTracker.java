package io.terpomo.pmitz.all.usage.tracker;

import io.terpomo.pmitz.all.usage.tracker.impl.FeatureUsageTrackerImpl;
import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageLimitVerifier;

import java.util.Map;
 
public interface FeatureUsageTracker {

    void recordFeatureUsage (Feature feature, UserGrouping userGrouping, Map<String, Long> requestedUnits);

    void reduceFeatureUsage (Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits);

    FeatureUsageInfo verifyLimits (Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits);

    FeatureUsageInfo getUsageInfo (Feature feature, UserGrouping userGrouping);

    class Builder {
        public static FeatureUsageTracker build(UsageLimitVerifier usageLimitVerifier) {
            return new FeatureUsageTrackerImpl(usageLimitVerifier, null);
        }
    }

}
