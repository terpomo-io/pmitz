/*
 * Copyright 2023-2024 the original author or authors.
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
import io.terpomo.pmitz.core.Feature;
import io.terpomo.pmitz.core.FeatureUsageInfo;
import io.terpomo.pmitz.core.subjects.UserGrouping;
import io.terpomo.pmitz.limits.UsageLimitVerifier;

public interface FeatureUsageTracker {

	void recordFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> requestedUnits);

	void reduceFeatureUsage(Feature feature, UserGrouping userGrouping, Map<String, Long> reducedUnits);

	FeatureUsageInfo verifyLimits(Feature feature, UserGrouping userGrouping, Map<String, Long> additionalUnits);

	FeatureUsageInfo getUsageInfo(Feature feature, UserGrouping userGrouping);

	class Builder {
		private Builder() {
			// disable instantiation of class
		}
		public static FeatureUsageTracker build(UsageLimitVerifier usageLimitVerifier) {
			return new FeatureUsageTrackerImpl(usageLimitVerifier, null);
		}
	}

}
