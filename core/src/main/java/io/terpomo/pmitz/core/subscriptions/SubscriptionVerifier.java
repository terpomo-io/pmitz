package io.terpomo.pmitz.core.subscriptions;

import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.subjects.UserGrouping;

public interface SubscriptionVerifier {

    boolean isFeatureAllowed(Feature feature, UserGrouping userGrouping);
}
