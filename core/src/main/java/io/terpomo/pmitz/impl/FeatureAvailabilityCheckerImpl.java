package io.terpomo.pmitz.impl;

import io.terpomo.pmitz.FeatureAvailabilityChecker;
import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureStatus;
import io.terpomo.pmitz.core.limits.UsageLimitVerifier;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;

public class FeatureAvailabilityCheckerImpl implements FeatureAvailabilityChecker {

    private final UsageLimitVerifier usageLimitVerifier;

    private final SubscriptionVerifier subscriptionVerifier;

    public FeatureAvailabilityCheckerImpl(UsageLimitVerifier usageLimitVerifier, SubscriptionVerifier subscriptionVerifier) {
        this.usageLimitVerifier = usageLimitVerifier;
        this.subscriptionVerifier = subscriptionVerifier;
    }

    @Override
    public FeatureStatus getFeatureStatus(Feature feature, UserGrouping userGrouping) {
        FeatureStatus status;
        if (!subscriptionVerifier.isFeatureAllowed(feature, userGrouping) ){
            status = FeatureStatus.NOT_ALLOWED;
        } else {
            boolean limitExceeded = feature.getLimitsIds().stream()
                    .anyMatch(limitId -> usageLimitVerifier.isLimitExceeded(feature, limitId, userGrouping));
            status = limitExceeded ? FeatureStatus.LIMIT_EXCEEDED : FeatureStatus.AVAILABLE;
        }
        return status;
    }
}
