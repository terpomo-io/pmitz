package io.terpomo.pmitz.core.subscriptions;

import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.subjects.UserGrouping;

import java.time.ZonedDateTime;

public class Subscription extends UserGrouping {

    private String subscriptionId;
    private SubscriptionStatus status;
    private ZonedDateTime expirationDate;



    private Plan plan;

    public Subscription(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public boolean isActive() {
        return SubscriptionStatus.ACTIVE == status;
    }

    public boolean isExpired() {
        return SubscriptionStatus.EXPIRED == status;
    }

    @Override
    public String getId() {
        return subscriptionId;
    }

    public Plan getPlan() {
        return plan;
    }
}
