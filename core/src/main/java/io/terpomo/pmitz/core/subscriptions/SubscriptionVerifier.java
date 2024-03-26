package io.terpomo.pmitz.core.subscriptions;

import java.util.Optional;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public interface SubscriptionVerifier {

	boolean isFeatureAllowed(Feature feature, UserGrouping userGrouping);

	Optional<Plan> findPlan(Feature feature, UserGrouping userGrouping);
}
