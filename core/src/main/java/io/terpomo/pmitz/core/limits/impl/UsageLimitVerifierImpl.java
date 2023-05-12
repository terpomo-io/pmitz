package io.terpomo.pmitz.core.limits.impl;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimitVerifier;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public class UsageLimitVerifierImpl implements UsageLimitVerifier {
    @Override
    public boolean isLimitExceeded(Feature feature, UserGrouping userGrouping) {
        return false;
    }
}
