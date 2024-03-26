package io.terpomo.pmitz;

import java.util.Map;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public interface FeatureUsageTracker {

	void recordFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> requestedUnits);

	void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits);

	FeatureUsageInfo verifyLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits);

	FeatureUsageInfo getUsageInfo(Feature feature, UserGrouping userGrouping);

}
