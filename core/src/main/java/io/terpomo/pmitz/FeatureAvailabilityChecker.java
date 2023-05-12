package io.terpomo.pmitz;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public interface FeatureAvailabilityChecker {

    FeatureStatus getFeatureStatus (Feature feature, UserGrouping userGrouping);

    default boolean isFeatureAvailable (Feature feature, UserGrouping userGrouping) {
        return getFeatureStatus(feature, userGrouping) == FeatureStatus.AVAILABLE;
    }
}
