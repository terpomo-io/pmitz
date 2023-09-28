package io.terpomo.pmitz.core.subscriptions;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.subjects.UserGrouping;

import java.util.Optional;

public interface SubscriptionVerifier {

    boolean isFeatureAllowed(Feature feature, UserGrouping userGrouping);

    Optional<Plan> findPlan (Feature feature, UserGrouping userGrouping);
}
