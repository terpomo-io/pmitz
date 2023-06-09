package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.subjects.UserGrouping;

import java.util.Optional;

public interface UsageLimitResolver {

    public Optional<UsageLimit> resolveUsageLimit (Feature feature, String usageLimitId, UserGrouping userGrouping);
}
