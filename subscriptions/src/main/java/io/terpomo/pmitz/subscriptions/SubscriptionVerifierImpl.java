package io.terpomo.pmitz.subscriptions;

import java.util.Optional;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.Plan;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;

public class SubscriptionVerifierImpl implements SubscriptionVerifier {

	private SubscriptionLifecycleManager subscriptionLifecycleManager;
	private SubscriptionFeatureManager subscriptionFeatureManager;

	@Override
	public boolean isFeatureAllowed(Feature feature, UserGrouping userGrouping) {

		var optSubscription = subscriptionLifecycleManager.find(userGrouping.getId());

		return optSubscription.isPresent()
				&& subscriptionFeatureManager.isFeatureIncluded(optSubscription.get(), feature);
	}

	@Override
	public Optional<Plan> findPlan(Feature feature, UserGrouping userGrouping) {
		var optSubscription = subscriptionLifecycleManager.find(userGrouping.getId());

		return optSubscription.isPresent() ? Optional.of(optSubscription.get().getPlan()) : Optional.empty();

		//TODO document design decision : Plans are valid only when there are subscriptions
	}
}
