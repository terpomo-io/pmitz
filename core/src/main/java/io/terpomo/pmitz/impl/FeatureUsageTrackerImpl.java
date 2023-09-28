package io.terpomo.pmitz.impl;

import io.terpomo.pmitz.core.exception.FeatureNotAllowedException;
import io.terpomo.pmitz.FeatureUsageTracker;
import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.limits.UsageLimitVerifier;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;

import java.util.Map;

public class FeatureUsageTrackerImpl implements FeatureUsageTracker {

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
        } //TODO add test
        usageLimitVerifier.recordFeatureUsage(feature, userGrouping, requestedUnits);
    }

    @Override
    public void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits) {
        usageLimitVerifier.reduceFeatureUsage(feature, userGrouping, reducedUnits);

    }

    @Override
    public FeatureStatus getFeatureStatus(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits) {
        FeatureStatus status;
        if (!subscriptionVerifier.isFeatureAllowed(feature, userGrouping) ){
            status = FeatureStatus.NOT_ALLOWED;
        } else {
            boolean withinLimits = usageLimitVerifier.isWithinLimits(feature, userGrouping, additionalUnits);
            status = withinLimits ? FeatureStatus.AVAILABLE : FeatureStatus.LIMIT_EXCEEDED ;
        }
        return status;
    }
}
