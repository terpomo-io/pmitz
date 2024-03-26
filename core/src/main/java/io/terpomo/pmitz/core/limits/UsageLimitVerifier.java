package io.terpomo.pmitz.core.limits;

import java.util.Map;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public interface UsageLimitVerifier {

	boolean isWithinLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits);

	void recordFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits);

	void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits);

	Map<String, Long> getLimitsRemainingUnits(Feature feature, UserGrouping userGrouping);
}
