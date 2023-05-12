package io.terpomo.pmitz.subscriptions;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subscriptions.Subscription;

public interface SubscriptionFeatureManager {

    boolean isFeatureExplicitlyAllowed (Subscription subscription, Feature feature);
}
