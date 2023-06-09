package io.terpomo.pmitz.limits.impl;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.limits.UsageLimitResolver;
import io.terpomo.pmitz.core.limits.UsageLimitVerifier;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageCounter;

import java.time.ZonedDateTime;
import java.util.Optional;

public class UsageLimitVerifierImpl implements UsageLimitVerifier {
    private UsageLimitResolver usageLimitResolver;
    private UsageCounter usageCounter;

    public UsageLimitVerifierImpl(UsageLimitResolver usageLimitResolver, UsageCounter usageCounter) {
        this.usageLimitResolver = usageLimitResolver;
        this.usageCounter = usageCounter;
    }

    @Override
    public boolean isLimitExceeded(Feature feature, String limitId, UserGrouping userGrouping) {
        Optional<UsageLimit> usageLimit = usageLimitResolver.resolveUsageLimit(feature, limitId, userGrouping);

        if (!usageLimit.isPresent()){
            return false;
        }

        Optional<Integer> count = usageCounter.getUsageCount(feature, limitId, userGrouping,
                usageLimit.get().getWindowStart(ZonedDateTime.now()), ZonedDateTime.now());

        return count.isPresent() &&  count.get() > usageLimit.get().getValue();
    }

    @Override
    public boolean isWithinLimit(Feature feature, String limitId, UserGrouping userGrouping, int additionalUnits) {
        Optional<UsageLimit> usageLimit = usageLimitResolver.resolveUsageLimit(feature, limitId, userGrouping);

        if (!usageLimit.isPresent()){
            return true;
        }

        Optional<Integer> count = usageCounter.getUsageCount(feature, limitId, userGrouping,
                usageLimit.get().getWindowStart(ZonedDateTime.now()), ZonedDateTime.now());

        return !count.isPresent() || (count.get() + additionalUnits <= usageLimit.get().getValue());
    }


}
