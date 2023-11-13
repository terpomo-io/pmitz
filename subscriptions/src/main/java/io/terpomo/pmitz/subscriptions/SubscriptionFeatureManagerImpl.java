package io.terpomo.pmitz.subscriptions;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.PlanRepository;
import io.terpomo.pmitz.core.subscriptions.Subscription;

public class SubscriptionFeatureManagerImpl implements SubscriptionFeatureManager{

    private PlanRepository planRepository;
    @Override
    public boolean isFeatureIncluded(Subscription subscription, Feature feature) {
        var plan = subscription.getPlan();

        return planRepository.isIncluded(plan, feature);
    }
}
