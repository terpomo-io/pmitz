package io.terpomo.pmitz.core.limits;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;

import java.util.Optional;

public interface UsageLimitResolver {

    public Opt
    ional<UsageLimit> resolveUsageLimit (Feature feature, UserGrouping userGrouping);
}
