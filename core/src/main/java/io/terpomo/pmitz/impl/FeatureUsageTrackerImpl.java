package io.terpomo.pmitz.impl;

import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.exception.FeatureNotAllowedException;
import io.terpomo.pmitz.FeatureUsageTracker;
import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.limits.UsageLimitVerifier;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class FeatureUsageTrackerImpl implements FeatureUsageTracker {

    private static final Long DEFAULT_ADDITIONAL_UNIT = 1L;

    private final UsageLimitVerifier usageLimitVerifier;

    private final SubscriptionVerifier subscriptionVerifier;

    public FeatureUsageTrackerImpl(UsageLimitVerifier usageLimitVerifier, SubscriptionVerifier subscriptionVerifier) {
        this.usageLimitVerifier = usageLimitVerifier;
        this.subscriptionVerifier = subscriptionVerifier;
    }

    @Override
    public void recordFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> requestedUnits) {
        if (!subscriptionVerifier.isFeatureAllowed(feature, userGrouping)){
            throw new FeatureNotAllowedException("Feature not allowed for userGrouping", feature, userGrouping);
        }
        usageLimitVerifier.recordFeatureUsage(feature, userGrouping, requestedUnits);
    }

    @Override
    public void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits) {
        usageLimitVerifier.reduceFeatureUsage(feature, userGrouping, reducedUnits);
    }

    @Override
    public FeatureUsageInfo verifyLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
        if (!subscriptionVerifier.isFeatureAllowed(feature, userGrouping)) {
            return new FeatureUsageInfo(FeatureStatus.NOT_ALLOWED, Collections.emptyMap());
        }

        var remainingUnits = usageLimitVerifier.getLimitsRemainingUnits(feature, userGrouping);

        BiFunction<String, Long, Long> remainingUnitsCalculator = (key, value) -> value - additionalUnits.getOrDefault(key, DEFAULT_ADDITIONAL_UNIT);

        var remainingUnitsAfterAdditions = remainingUnits.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, mapEntry -> remainingUnitsCalculator.apply(mapEntry.getKey(), mapEntry.getValue())));

        var anyLimitExceeded = remainingUnitsAfterAdditions.entrySet().stream().anyMatch(mapEntry -> mapEntry.getValue() < 0);

        return new FeatureUsageInfo(anyLimitExceeded ? FeatureStatus.LIMIT_EXCEEDED : FeatureStatus.AVAILABLE, remainingUnitsAfterAdditions);
    }

    @Override
    public FeatureUsageInfo getUsageInfo(Feature feature, UserGrouping userGrouping) {
        if (!subscriptionVerifier.isFeatureAllowed(feature, userGrouping)) {
            return new FeatureUsageInfo(FeatureStatus.NOT_ALLOWED, Collections.emptyMap());
        }

        var remainingUnits = usageLimitVerifier.getLimitsRemainingUnits(feature, userGrouping);

        var anyLimitExceeded = remainingUnits.entrySet().stream().anyMatch(mapEntry -> mapEntry.getValue() < 0);

        return new FeatureUsageInfo(anyLimitExceeded ? FeatureStatus.LIMIT_EXCEEDED : FeatureStatus.AVAILABLE, remainingUnits);
    }

}
