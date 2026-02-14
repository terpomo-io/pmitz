/*
 * Copyright 2023-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.terpomo.pmitz.all.usage.tracker;

import java.util.Map;

import io.terpomo.pmitz.all.usage.tracker.impl.FeatureUsageTrackerImpl;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.core.subscriptions.FeatureRef;
import io.terpomo.pmitz.core.subscriptions.SubscriptionVerifier;
import io.terpomo.pmitz.limits.LimitVerifier;

public interface FeatureUsageTracker {

	void recordFeatureUsage(FeatureRef featureRef, UserGrouping userGrouping, Map<String, Long> requestedUnits);

	void reduceFeatureUsage(FeatureRef featureRef, UserGrouping userGrouping, Map<String, Long> reducedUnits);

	FeatureUsageInfo verifyLimits(FeatureRef featureRef, UserGrouping userGrouping, Map<String, Long> additionalUnits);

	FeatureUsageInfo getUsageInfo(FeatureRef featureRef, UserGrouping userGrouping);

	class Builder {
		private Builder() {
			// disable instantiation of class
		}
		public static FeatureUsageTracker build(LimitVerifier limitVerifier, SubscriptionVerifier subscriptionVerifier) {
			return new FeatureUsageTrackerImpl(limitVerifier, subscriptionVerifier);
		}
	}

}
