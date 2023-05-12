package io.terpomo.pmitz.core.limits;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public interface UsageLimitVerifier {

    boolean isLimitExceeded(Feature feature, UserGrouping userGrouping);

}
