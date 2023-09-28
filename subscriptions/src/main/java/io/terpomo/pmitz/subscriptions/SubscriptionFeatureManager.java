package io.terpomo.pmitz.subscriptions;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subscriptions.Subscription;

public interface SubscriptionFeatureManager {

    boolean isFeatureIncluded (Subscription subscription, Feature feature);

    //TODO document design decision :
    // A feature cannot be added explicitly to a subscription
    // It is tedious to start managing features "a la carte" instead of by plan, and it requires storage
}
