package io.terpomo.pmitz.limits.impl;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageLimitResolver;

import java.util.Optional;

public class UsageLimitResolverImpl implements UsageLimitResolver {

    @Override
    public Optional<UsageLimit> resolveUsageLimit(Feature feature, String usageLimitId, UserGrouping userGrouping) {
        //TODO implement
        // 1- find limit specific to userGrouping
        // 2- find limit for plan --> 2.1 find plan, 2.2 find limit for plan
        // 3- find global limit
        return Optional.empty();
    }
}
