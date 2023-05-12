package io.terpomo.pmitz.limits;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.limits.UsageLimit;

import java.util.Optional;

public interface SubscriptionLimitsDefinition {

    void setLimit (Feature feature, String subscriptionId, UsageLimit limit);

    void removeLimit (Feature feature, String subscriptionId);

    Optional<UsageLimit> getLimit (Feature feature, String subscriptionId);

}
