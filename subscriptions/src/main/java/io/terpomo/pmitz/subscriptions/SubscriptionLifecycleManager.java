package io.terpomo.pmitz.subscriptions;

import io.terpomo.pmitz.core.subscriptions.Subscription;
import io.terpomo.pmitz.core.subscriptions.SubscriptionStatus;

public interface SubscriptionLifecycleManager {

    void create(Subscription subscription);

    void find(String subscriptionId);

    void updateStatus(Subscription subscription, SubscriptionStatus newStatus);

    default void activate (Subscription subscription) {
        updateStatus(subscription, SubscriptionStatus.ACTIVE);
    }

    default void cancel (Subscription subscription) {
        updateStatus(subscription, SubscriptionStatus.CANCELLED);
    }

    default void terminate (Subscription subscription) {
        updateStatus(subscription, SubscriptionStatus.TERMINATED);
    }
}
