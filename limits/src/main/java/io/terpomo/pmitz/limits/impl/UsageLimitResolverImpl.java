package io.terpomo.pmitz.limits.impl;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureRepository;
import io.terpomo.pmitz.core.ProductRepository;
import io.terpomo.pmitz.core.limits.UsageLimit;
import io.terpomo.pmitz.core.limits.UserLimitRepository;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageLimitResolver;

import java.util.Optional;

public class UsageLimitResolverImpl implements UsageLimitResolver {

    private FeatureRepository featureRepository;
    private UserLimitRepository userLimitRepository;

    @Override
    public Optional<UsageLimit> resolveUsageLimit(Feature feature, String usageLimitId, UserGrouping userGrouping) {
        //TODO verify interfaces' specs
        // 1- find limit specific to userGrouping
        // 2- find limit for plan --> 2.1 find plan, 2.2 find limit for plan
        // 3- find global limit

        return userLimitRepository.findUsageLimit(feature, usageLimitId, userGrouping)
                .or(() -> featureRepository.getGlobalLimit(feature, usageLimitId));

    }
}
