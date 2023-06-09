package io.terpomo.pmitz.core.limits;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public interface UsageLimitVerifier {

    boolean isLimitExceeded(Feature feature, String limitId, UserGrouping userGrouping);

    boolean isWithinLimit(Feature feature, String limitId, UserGrouping userGrouping, int additionalUnits);

}
